package com.shop.service;

import com.shop.dto.AfterSaleApplyRequest;
import com.shop.dto.AfterSaleDetailVO;
import com.shop.entity.*;
import com.shop.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 售后服务
 */
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleService.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleLogMapper afterSaleLogMapper;
    private final AfterSaleSupplementMapper supplementMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public AfterSale apply(Long userId, AfterSaleApplyRequest req) {
        if (req.getType() == null || (req.getType() < 1 || req.getType() > 3)) {
            throw new IllegalArgumentException("售后类型无效");
        }
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("请填写售后原因");
        }
        if (req.getVoucherImages() != null && req.getVoucherImages().size() > 3) {
            throw new IllegalArgumentException("凭证图片最多3张");
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(0L);
        OrderItem orderItem = null;
        OrderMain order = null;
        List<OrderMain> userOrders = orderMainMapper.selectByUserId(userId);
        for (OrderMain o : userOrders) {
            List<OrderItem> its = orderItemMapper.selectByOrderId(o.getId());
            for (OrderItem it : its) {
                if (it.getId().equals(req.getOrderItemId())) {
                    orderItem = it;
                    order = o;
                    break;
                }
            }
            if (orderItem != null) break;
        }
        if (orderItem == null) {
            throw new IllegalArgumentException("订单明细不存在");
        }
        if (order.getStatus() != 3) {
            throw new IllegalArgumentException("仅已完成订单可发起售后");
        }
        int existsCount = afterSaleMapper.countByOrderItemId(orderItem.getId());
        if (existsCount > 0) {
            throw new IllegalArgumentException("该商品已存在售后申请中");
        }
        String no = "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", SEQ.incrementAndGet() % 10000);
        AfterSale as = new AfterSale();
        as.setAfterSaleNo(no);
        as.setUserId(userId);
        as.setOrderId(order.getId());
        as.setOrderItemId(orderItem.getId());
        as.setProductId(orderItem.getProductId());
        as.setProductName(orderItem.getProductName());
        as.setProductImage(orderItem.getProductImage());
        as.setType(req.getType());
        as.setReason(req.getReason());
        as.setDescription(req.getDescription());
        if (req.getVoucherImages() != null && !req.getVoucherImages().isEmpty()) {
            as.setVoucherImages(String.join(",", req.getVoucherImages()));
        }
        as.setOriginalPrice(orderItem.getPrice());
        as.setOriginalQuantity(orderItem.getQuantity());
        as.setOriginalTotalAmount(orderItem.getTotalAmount());
        as.setStatus(0);
        afterSaleMapper.insert(as);

        User user = userMapper.selectById(userId);
        addLog(as.getId(), userId, user != null ? user.getNickname() != null ? user.getNickname() : user.getUsername() : "用户", "user",
                "CREATE", null, 0, "发起售后申请：" + typeText(req.getType()));
        log.info("AfterSale applied: id={}, no={}, userId={}", as.getId(), no, userId);
        return afterSaleMapper.selectById(as.getId());
    }

    public List<AfterSale> listByUser(Long userId) {
        return afterSaleMapper.selectByUserId(userId);
    }

    public AfterSaleDetailVO getDetail(Long id, Long userId) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) return null;
        if (userId != null && !as.getUserId().equals(userId)) return null;
        AfterSaleDetailVO vo = new AfterSaleDetailVO();
        vo.setAfterSale(as);
        vo.setLogs(afterSaleLogMapper.selectByAfterSaleId(id));
        if (as.getSupplementId() != null) {
            vo.setSupplement(supplementMapper.selectById(as.getSupplementId()));
        }
        return vo;
    }

    public List<AfterSale> listAll(Integer status) {
        return afterSaleMapper.selectAll(status);
    }

    public AfterSaleDetailVO getAdminDetail(Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) return null;
        AfterSaleDetailVO vo = new AfterSaleDetailVO();
        vo.setAfterSale(as);
        vo.setLogs(afterSaleLogMapper.selectByAfterSaleId(id));
        if (as.getSupplementId() != null) {
            vo.setSupplement(supplementMapper.selectById(as.getSupplementId()));
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, BigDecimal refundAmount, Long adminId, String adminName) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (as.getStatus() != 0) throw new IllegalStateException("当前状态不可审核");
        Integer type = as.getType();
        if (type == 1 || type == 3) {
            if (refundAmount == null) throw new IllegalArgumentException("请填写退款金额");
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("退款金额必须大于0");
            if (refundAmount.compareTo(as.getOriginalTotalAmount()) > 0) {
                throw new IllegalArgumentException("退款金额不能超过实付金额 ¥" + as.getOriginalTotalAmount());
            }
            afterSaleMapper.updateApprove(id, refundAmount);
            addLog(id, adminId, adminName, "admin", "APPROVE", 0, 1, "审核通过，退款金额：¥" + refundAmount);
            if (type == 1) {
                afterSaleMapper.updateStatus(id, 3);
                addLog(id, null, "系统", "system", "APPROVE", 1, 3, "等待用户寄回商品");
            } else {
                afterSaleMapper.updateStatus(id, 5);
                addLog(id, null, "系统", "system", "REFUND", 1, 5, "仅退款完成：¥" + refundAmount);
            }
        } else if (type == 2) {
            afterSaleMapper.updateStatus(id, 3);
            addLog(id, adminId, adminName, "admin", "APPROVE", 0, 3, "换货审核通过，等待用户寄回商品");
        }
        log.info("AfterSale approved: id={}, adminId={}", id, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String rejectReason, Long adminId, String adminName) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (as.getStatus() != 0) throw new IllegalStateException("当前状态不可审核");
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写拒绝原因");
        }
        afterSaleMapper.updateReject(id, rejectReason);
        addLog(id, adminId, adminName, "admin", "REJECT", 0, 2, "审核拒绝：" + rejectReason);
        log.info("AfterSale rejected: id={}, adminId={}", id, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void userShipReturn(Long id, String company, String trackingNo, Long userId) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (!as.getUserId().equals(userId)) throw new IllegalArgumentException("无权限");
        if (as.getStatus() != 3) throw new IllegalStateException("当前状态不可填写物流");
        if (company == null || company.trim().isEmpty() || trackingNo == null || trackingNo.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写完整物流信息");
        }
        afterSaleMapper.updateReturnShip(id, company, trackingNo);
        User user = userMapper.selectById(userId);
        String name = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "用户";
        addLog(id, userId, name, "user", "USER_SHIP", 3, 3, "用户寄回：" + company + " " + trackingNo + "，等待仓库确认");
        log.info("AfterSale user shipped: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void warehouseConfirm(Long id, Long adminId, String adminName) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (as.getStatus() != 3 || as.getReturnTrackingNo() == null) {
            throw new IllegalStateException("用户尚未寄回，无法确认");
        }
        Integer type = as.getType();
        if (type == 1) {
            afterSaleMapper.updateStatus(id, 5);
            addLog(id, adminId, adminName, "admin", "WAREHOUSE_CONFIRM", 3, 5, "仓库确认收货，已退款：¥" + as.getRefundAmount());
        } else if (type == 2) {
            afterSaleMapper.updateStatus(id, 4);
            addLog(id, adminId, adminName, "admin", "WAREHOUSE_CONFIRM", 3, 4, "仓库确认收货，等待换货发出");
        }
        log.info("AfterSale warehouse confirmed: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminShipExchange(Long id, String company, String trackingNo, BigDecimal priceDiff, Long adminId, String adminName) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (as.getStatus() != 4) throw new IllegalStateException("当前状态不可发货");
        if (company == null || company.trim().isEmpty() || trackingNo == null || trackingNo.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写完整物流信息");
        }
        Long supplementId = null;
        if (priceDiff != null && priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            String supNo = "SUP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", SEQ.incrementAndGet() % 10000);
            AfterSaleSupplement sup = new AfterSaleSupplement();
            sup.setSupplementNo(supNo);
            sup.setAfterSaleId(id);
            sup.setUserId(as.getUserId());
            sup.setPriceDiff(priceDiff);
            sup.setStatus(0);
            supplementMapper.insert(sup);
            supplementId = sup.getId();
            addLog(id, null, "系统", "system", null, 4, 4, "生成补款单：" + supNo + "，差价 ¥" + priceDiff + "，请先支付补款");
            afterSaleMapper.updateSupplementId(id, supplementId);
            log.info("Supplement created: id={}, afterSaleId={}", supplementId, id);
        } else {
            afterSaleMapper.updateExchangeShip(id, company, trackingNo, null);
            addLog(id, adminId, adminName, "admin", "EXCHANGE_SHIP", 4, 6, "换货发出：" + company + " " + trackingNo);
            log.info("AfterSale exchange shipped: id={}", id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void paySupplement(Long supplementId, Long userId) {
        AfterSaleSupplement sup = supplementMapper.selectById(supplementId);
        if (sup == null) throw new IllegalArgumentException("补款单不存在");
        if (!sup.getUserId().equals(userId)) throw new IllegalArgumentException("无权限");
        if (sup.getStatus() != 0) throw new IllegalStateException("补款单状态异常");
        supplementMapper.updateStatus(supplementId, 1, LocalDateTime.now());
        AfterSale as = afterSaleMapper.selectById(sup.getAfterSaleId());
        User user = userMapper.selectById(userId);
        String name = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "用户";
        addLog(sup.getAfterSaleId(), userId, name, "user", "PAY_SUPPLEMENT", 4, 4, "补款完成：¥" + sup.getPriceDiff());
        if (as != null && as.getExchangeCompany() != null && as.getExchangeTrackingNo() != null) {
            afterSaleMapper.updateStatus(as.getId(), 6);
            addLog(sup.getAfterSaleId(), null, "系统", "system", "EXCHANGE_SHIP", 4, 6,
                    "换货已发出：" + as.getExchangeCompany() + " " + as.getExchangeTrackingNo());
        }
        log.info("Supplement paid: id={}", supplementId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long userId) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new IllegalArgumentException("售后不存在");
        if (!as.getUserId().equals(userId)) throw new IllegalArgumentException("无权限");
        if (as.getStatus() != 0 && as.getStatus() != 3) {
            throw new IllegalStateException("当前状态不可取消");
        }
        int oldStatus = as.getStatus();
        afterSaleMapper.updateStatus(id, 7);
        User user = userMapper.selectById(userId);
        String name = user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "用户";
        addLog(id, userId, name, "user", "CANCEL", oldStatus, 7, "用户取消售后申请");
        log.info("AfterSale cancelled: id={}", id);
    }

    private void addLog(Long afterSaleId, Long operatorId, String operatorName, String operatorRole,
                        String action, Integer statusFrom, Integer statusTo, String remark) {
        AfterSaleLog l = new AfterSaleLog();
        l.setAfterSaleId(afterSaleId);
        l.setOperatorId(operatorId);
        l.setOperatorName(operatorName);
        l.setOperatorRole(operatorRole);
        l.setAction(action);
        l.setStatusFrom(statusFrom);
        l.setStatusTo(statusTo);
        l.setRemark(remark);
        afterSaleLogMapper.insert(l);
    }

    public static String typeText(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "退货退款";
            case 2: return "换货";
            case 3: return "仅退款";
            default: return "未知";
        }
    }

    public static String statusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "待寄回";
            case 4: return "仓库确认";
            case 5: return "已退款";
            case 6: return "已换货发出";
            case 7: return "已取消";
            default: return "未知";
        }
    }

    public List<AfterSaleSupplement> listSupplementsByUser(Long userId) {
        return supplementMapper.selectByUserId(userId);
    }
}
