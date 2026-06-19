package com.shop.service;

import com.shop.dto.ExchangeCreateRequest;
import com.shop.entity.PointsExchangeOrder;
import com.shop.entity.PointsProduct;
import com.shop.mapper.PointsExchangeOrderMapper;
import com.shop.mapper.PointsProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PointsExchangeService {

    private static final Logger log = LoggerFactory.getLogger(PointsExchangeService.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final PointsProductMapper pointsProductMapper;
    private final PointsExchangeOrderMapper pointsExchangeOrderMapper;
    private final PointsLevelService pointsLevelService;

    public List<PointsProduct> listOnShelfProducts() {
        return pointsProductMapper.selectOnShelf();
    }

    public List<PointsProduct> listAllProducts() {
        return pointsProductMapper.selectAll();
    }

    public PointsProduct getProduct(Long id) {
        return pointsProductMapper.selectById(id);
    }

    public PointsProduct createProduct(PointsProduct product) {
        pointsProductMapper.insert(product);
        return pointsProductMapper.selectById(product.getId());
    }

    public PointsProduct updateProduct(PointsProduct product) {
        pointsProductMapper.updateById(product);
        return pointsProductMapper.selectById(product.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PointsExchangeOrder createExchangeOrder(Long userId, ExchangeCreateRequest req) {
        PointsProduct pp = pointsProductMapper.selectById(req.getPointsProductId());
        if (pp == null || pp.getStatus() != 1) {
            throw new IllegalArgumentException("兑换商品不存在或已下架");
        }
        if (pp.getStock() <= 0) {
            throw new IllegalStateException("兑换商品库存不足");
        }
        int userPoints = pointsLevelService.getUserPoints(userId);
        if (userPoints < pp.getPointsRequired()) {
            throw new IllegalStateException("积分不足，无法兑换");
        }

        int stockUpdated = pointsProductMapper.updateStock(pp.getId(), 1);
        if (stockUpdated <= 0) {
            throw new IllegalStateException("兑换失败，库存不足");
        }

        String exchangeNo = "E" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet() % 10000);

        PointsExchangeOrder order = new PointsExchangeOrder();
        order.setExchangeNo(exchangeNo);
        order.setUserId(userId);
        order.setPointsProductId(pp.getId());
        order.setProductName(pp.getName());
        order.setProductImage(pp.getImage());
        order.setPointsCost(pp.getPointsRequired());
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        order.setStatus(0);
        pointsExchangeOrderMapper.insert(order);

        pointsLevelService.deductPointsForExchange(userId, pp.getPointsRequired(), order.getId());

        log.info("Exchange order created: exchangeNo={}, userId={}, productId={}, points={}",
                exchangeNo, userId, pp.getId(), pp.getPointsRequired());
        return pointsExchangeOrderMapper.selectById(order.getId());
    }

    public List<PointsExchangeOrder> listMyOrders(Long userId) {
        return pointsExchangeOrderMapper.selectByUserId(userId);
    }

    public PointsExchangeOrder getOrderById(Long id, Long userId) {
        PointsExchangeOrder order = pointsExchangeOrderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    public List<PointsExchangeOrder> listAllOrders() {
        return pointsExchangeOrderMapper.selectAll();
    }

    public PointsExchangeOrder getAdminOrder(Long id) {
        return pointsExchangeOrderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long id, String expressCompany, String expressNo) {
        PointsExchangeOrder order = pointsExchangeOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("兑换单不存在");
        }
        if (order.getStatus() != 0) {
            throw new IllegalStateException("当前状态不支持发货");
        }
        pointsExchangeOrderMapper.updateExpress(id, expressCompany, expressNo);
        log.info("Exchange order shipped: id={}, company={}, no={}", id, expressCompany, expressNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long id) {
        PointsExchangeOrder order = pointsExchangeOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("兑换单不存在");
        }
        if (order.getStatus() != 1) {
            throw new IllegalStateException("当前状态不支持完成");
        }
        pointsExchangeOrderMapper.updateStatus(id, 2);
        log.info("Exchange order completed: id={}", id);
    }
}
