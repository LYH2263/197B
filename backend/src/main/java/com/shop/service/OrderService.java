package com.shop.service;

import com.shop.dto.CouponCalculateRequest;
import com.shop.dto.CouponCalculateResult;
import com.shop.dto.OrderCreateRequest;
import com.shop.dto.PromotionCalculateRequest;
import com.shop.dto.PromotionCalculateResult;
import com.shop.dto.SeckillOrderCreateRequest;
import com.shop.entity.CartItem;
import com.shop.entity.OrderItem;
import com.shop.entity.OrderMain;
import com.shop.entity.Product;
import com.shop.entity.SeckillSession;
import com.shop.entity.SeckillToken;
import com.shop.mapper.CartItemMapper;
import com.shop.mapper.OrderItemMapper;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.SeckillSessionMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final CouponService couponService;
    private final PromotionService promotionService;
    private final PointsLevelService pointsLevelService;
    private final SeckillService seckillService;
    private final SeckillSessionMapper seckillSessionMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderMain create(Long userId, OrderCreateRequest req) {
        List<CartItem> checked = cartItemMapper.selectCheckedByUserId(userId);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("请先勾选要结算的商品");
        }

        List<PromotionCalculateRequest.OrderItemDTO> promotionItems = new ArrayList<>();
        List<CouponCalculateRequest.OrderItemDTO> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : checked) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || p.getStock() < item.getQuantity()) {
                throw new IllegalStateException("商品 " + (p != null ? p.getName() : item.getProductId()) + " 库存不足");
            }
            BigDecimal itemTotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            PromotionCalculateRequest.OrderItemDTO pItem = new PromotionCalculateRequest.OrderItemDTO();
            pItem.setProductId(p.getId());
            pItem.setCategoryId(p.getCategoryId());
            pItem.setPrice(p.getPrice());
            pItem.setQuantity(item.getQuantity());
            promotionItems.add(pItem);

            CouponCalculateRequest.OrderItemDTO dto = new CouponCalculateRequest.OrderItemDTO();
            dto.setProductId(p.getId());
            dto.setCategoryId(p.getCategoryId());
            dto.setPrice(p.getPrice());
            dto.setQuantity(item.getQuantity());
            orderItems.add(dto);
        }

        // ========== 叠加规则：【先满减 → 再优惠券 → 再积分】 ==========
        // 满减是平台级的商品促销，属于"降价"性质的基础优惠；
        // 优惠券是用户领取的专属权益，应在满减后的基础上进一步抵扣。
        // 这样避免了：满减降低订单金额后，优惠券门槛"被降低"的不公平情况。

        // 第一步：计算满减（PromotionService.calculate 内部自动取最优档位）
        PromotionCalculateRequest promoReq = new PromotionCalculateRequest();
        promoReq.setItems(promotionItems);
        PromotionCalculateResult promoResult = promotionService.calculate(promoReq, false);
        BigDecimal promotionDiscount = promoResult.getPromotionDiscount() != null
                ? promoResult.getPromotionDiscount() : BigDecimal.ZERO;
        Long promotionId = promoResult.getPromotionId();
        // 满减后金额
        BigDecimal afterPromotion = totalAmount.subtract(promotionDiscount);
        if (afterPromotion.compareTo(BigDecimal.ZERO) < 0) afterPromotion = BigDecimal.ZERO;

        // 第二步：计算优惠券（基于满减后的金额重新组装订单商品用于计算）
        // 注意：优惠券的门槛判定基于满减后的金额
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Long couponId = null;
        if (req.getUserCouponId() != null) {
            CouponCalculateRequest calcReq = new CouponCalculateRequest();
            calcReq.setItems(orderItems);
            calcReq.setUserCouponId(req.getUserCouponId());
            // 这里需要修改 CouponService.calculate 来支持传入前置减免后的金额
            // 暂时按原逻辑计算，后续可在 CouponService 中扩展 afterPromotion 参数
            CouponCalculateResult calcResult = couponService.calculate(userId, calcReq);
            if (calcResult.getSelectedCoupon() == null || !calcResult.getSelectedCoupon().getAvailable()) {
                throw new IllegalArgumentException("选择的优惠券不可用");
            }
            // 优惠券减免不能超过满减后的剩余金额
            couponDiscount = calcResult.getDiscountAmount().min(afterPromotion);
            couponId = calcResult.getSelectedCoupon().getCouponId();
        }
        // 满减+优惠券后金额
        BigDecimal afterCoupon = afterPromotion.subtract(couponDiscount);
        if (afterCoupon.compareTo(BigDecimal.ZERO) < 0) afterCoupon = BigDecimal.ZERO;

        // 第三步：计算积分（基于满减+优惠券后的金额）
        int pointsToUse = req.getPointsToUse() != null ? req.getPointsToUse() : 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        int actualPointsUsed = 0;
        if (pointsToUse > 0) {
            var calc = pointsLevelService.calculateForOrder(userId, afterCoupon);
            if (pointsToUse > calc.getMaxPointsUsable()) {
                throw new IllegalArgumentException("使用积分超过最大可抵扣额度");
            }
            actualPointsUsed = pointsToUse;
            pointsDiscount = new BigDecimal(actualPointsUsed)
                    .divide(new BigDecimal(PointsLevelService.POINTS_PER_YUAN), 2, RoundingMode.DOWN);
        }

        BigDecimal finalAmount = afterCoupon.subtract(pointsDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        String orderNo = "O" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet() % 10000);
        OrderMain order = new OrderMain();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(finalAmount);
        order.setDiscountAmount(couponDiscount);
        order.setCouponId(couponId);
        order.setPromotionId(promotionId);
        order.setPromotionDiscount(promotionDiscount);
        order.setPointsDiscount(pointsDiscount);
        order.setPointsUsed(actualPointsUsed);
        order.setPointsEarned(0);
        order.setStatus(0);
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        orderMainMapper.insert(order);

        // 锁定满减活动快照（下单时写入）
        if (promotionId != null && promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
            promotionService.lockSnapshotForOrder(order.getId(), promoResult);
        }
        if (req.getUserCouponId() != null) {
            couponService.useCoupon(req.getUserCouponId(), userId, order.getId());
        }
        if (actualPointsUsed > 0) {
            pointsLevelService.deductPointsForOrder(userId, actualPointsUsed, order.getId());
        }
        for (CartItem item : checked) {
            Product p = productMapper.selectById(item.getProductId());
            productMapper.updateStock(p.getId(), item.getQuantity());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductImage(p.getMainImage());
            oi.setPrice(p.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalAmount(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
            cartItemMapper.deleteByUserIdAndProductId(userId, item.getProductId());
        }
        log.info("Order created: orderNo={}, userId={}, promotionDiscount={}, couponDiscount={}, pointsUsed={}",
                orderNo, userId, promotionDiscount, couponDiscount, actualPointsUsed);
        return orderMainMapper.selectById(order.getId());
    }

    public List<OrderMain> listByUserId(Long userId) {
        return orderMainMapper.selectByUserId(userId);
    }

    public OrderMain getById(Long id, Long userId) {
        OrderMain order = orderMainMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("订单状态不允许支付");
        }
        orderMainMapper.updateStatus(orderId, 1);
        log.info("Order paid: orderId={}", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new IllegalArgumentException("仅已付款订单可发货");
        }
        orderMainMapper.updateStatus(orderId, 2);
        log.info("Order shipped: orderId={}", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new IllegalArgumentException("仅已发货订单可确认收货");
        }
        orderMainMapper.updateStatus(orderId, 3);

        BigDecimal paidAmount = order.getTotalAmount();
        int awarded = pointsLevelService.awardPointsByOrder(userId, paidAmount, orderId);

        orderMainMapper.updatePointsEarned(orderId, awarded);

        log.info("Order confirmed: orderId={}, pointsEarned={}", orderId, awarded);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("仅待付款订单可取消");
        }
        orderMainMapper.updateStatus(orderId, 4);
        log.info("Order cancelled: orderId={}", orderId);
    }

    public List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }

    public List<OrderMain> listAll() {
        return orderMainMapper.selectAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderMain createSeckillOrder(Long userId, SeckillOrderCreateRequest req) {
        SeckillToken token = seckillService.validateAndMarkToken(req.getToken(), req.getSessionId(), userId);

        SeckillSession session = seckillSessionMapper.selectById(req.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("秒杀场次不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            throw new IllegalStateException("活动未开始");
        }
        if (now.isAfter(session.getEndTime())) {
            throw new IllegalStateException("活动已结束");
        }

        int userBought = seckillService.getUserBoughtCount(req.getSessionId(), userId);
        if (userBought >= session.getPerUserLimit()) {
            throw new IllegalStateException("您已达到本场限购数量（" + session.getPerUserLimit() + " 件）");
        }

        int buyQuantity = 1;
        int allowed = session.getPerUserLimit() - userBought;
        if (buyQuantity > allowed) {
            buyQuantity = allowed;
        }
        if (buyQuantity <= 0) {
            throw new IllegalStateException("超出限购数量");
        }

        boolean stockDeducted = seckillService.deductStock(req.getSessionId(), buyQuantity);
        if (!stockDeducted) {
            throw new IllegalStateException("很抱歉，商品已售罄");
        }

        Product p = productMapper.selectById(session.getProductId());
        if (p == null) {
            throw new IllegalStateException("商品不存在");
        }

        if (p.getStock() < buyQuantity) {
            throw new IllegalStateException("商品主库存不足");
        }
        productMapper.updateStock(p.getId(), buyQuantity);

        BigDecimal seckillTotal = session.getSeckillPrice().multiply(BigDecimal.valueOf(buyQuantity));

        String orderNo = "OS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet() % 10000);
        OrderMain order = new OrderMain();
        order.setOrderNo(orderNo);
        order.setOrderType("seckill");
        order.setSeckillSessionId(req.getSessionId());
        order.setUserId(userId);
        order.setTotalAmount(seckillTotal);
        order.setDiscountAmount(p.getPrice().multiply(BigDecimal.valueOf(buyQuantity)).subtract(seckillTotal));
        order.setCouponId(null);
        order.setPointsDiscount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setPointsEarned(0);
        order.setStatus(0);
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        orderMainMapper.insert(order);

        OrderItem oi = new OrderItem();
        oi.setOrderId(order.getId());
        oi.setProductId(p.getId());
        oi.setProductName(p.getName());
        oi.setProductImage(p.getMainImage());
        oi.setPrice(session.getSeckillPrice());
        oi.setQuantity(buyQuantity);
        oi.setTotalAmount(seckillTotal);
        orderItemMapper.insert(oi);

        log.info("Seckill order created: orderNo={}, sessionId={}, userId={}, quantity={}",
                orderNo, req.getSessionId(), userId, buyQuantity);
        return orderMainMapper.selectById(order.getId());
    }
}
