package com.shop.service;

import com.shop.dto.*;
import com.shop.entity.*;
import com.shop.mapper.*;
import com.shop.service.tracking.TrackingAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentMapper shipmentMapper;
    private final ShipmentTraceMapper shipmentTraceMapper;
    private final DeliveryUrgeMapper deliveryUrgeMapper;
    private final DeliveryIssueMapper deliveryIssueMapper;
    private final OrderMainMapper orderMainMapper;
    private final TrackingAdapter trackingAdapter;

    private static final List<ExpressCompanyVO> EXPRESS_COMPANIES = Arrays.asList(
            new ExpressCompanyVO("SF", "顺丰速运"),
            new ExpressCompanyVO("YTO", "圆通速递"),
            new ExpressCompanyVO("ZTO", "中通快递"),
            new ExpressCompanyVO("STO", "申通快递"),
            new ExpressCompanyVO("YD", "韵达速递"),
            new ExpressCompanyVO("JD", "京东物流"),
            new ExpressCompanyVO("EMS", "EMS邮政"),
            new ExpressCompanyVO("DBL", "德邦快递")
    );

    public List<ExpressCompanyVO> listExpressCompanies() {
        return EXPRESS_COMPANIES;
    }

    @Transactional(rollbackFor = Exception.class)
    public Shipment createShipment(ShipmentCreateRequest req) {
        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new IllegalArgumentException("仅已付款订单可发货");
        }
        Shipment existing = shipmentMapper.selectByOrderId(req.getOrderId());
        if (existing != null) {
            throw new IllegalArgumentException("该订单已发货");
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(req.getOrderId());
        shipment.setOrderNo(order.getOrderNo());
        shipment.setExpressCompanyCode(req.getExpressCompanyCode());
        shipment.setExpressCompanyName(req.getExpressCompanyName());
        shipment.setTrackingNo(req.getTrackingNo());
        shipment.setStatus(0);
        shipment.setShippedAt(LocalDateTime.now());
        shipmentMapper.insert(shipment);

        orderMainMapper.updateStatus(req.getOrderId(), 2);

        ShipmentTrace firstTrace = new ShipmentTrace();
        firstTrace.setShipmentId(shipment.getId());
        firstTrace.setStatus(0);
        firstTrace.setLocation("仓库");
        firstTrace.setDescription("商家已发货，等待快递员揽收");
        firstTrace.setTraceTime(shipment.getShippedAt());
        shipmentTraceMapper.insert(firstTrace);

        log.info("Shipment created: orderId={}, company={}, trackingNo={}",
                req.getOrderId(), req.getExpressCompanyName(), req.getTrackingNo());
        return shipmentMapper.selectById(shipment.getId());
    }

    public Shipment getShipmentByOrderId(Long orderId) {
        return shipmentMapper.selectByOrderId(orderId);
    }

    public ShipmentDetailVO getShipmentDetailByOrderId(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        Shipment shipment = shipmentMapper.selectByOrderId(orderId);
        if (shipment == null) {
            return null;
        }
        return buildDetailVO(shipment, userId);
    }

    public ShipmentDetailVO getShipmentDetail(Long shipmentId) {
        Shipment shipment = shipmentMapper.selectById(shipmentId);
        if (shipment == null) {
            return null;
        }
        return buildDetailVO(shipment, null);
    }

    private ShipmentDetailVO buildDetailVO(Shipment shipment, Long userId) {
        Integer adapterStatus = trackingAdapter.getCurrentStatus(shipment);
        if (!adapterStatus.equals(shipment.getStatus()) && shipment.getStatus() < 4) {
            shipmentMapper.updateStatus(shipment.getId(), adapterStatus);
            shipment.setStatus(adapterStatus);
            if (adapterStatus == 4 && shipment.getSignedAt() == null) {
                shipment.setSignedAt(LocalDateTime.now());
                shipmentMapper.updateById(shipment);
            }
        }

        List<ShipmentTrace> traces = shipmentTraceMapper.selectByShipmentId(shipment.getId());
        List<ShipmentTrace> adapterTraces = trackingAdapter.fetchTraces(shipment);
        if (adapterTraces.size() > traces.size()) {
            shipmentTraceMapper.deleteByShipmentId(shipment.getId());
            for (ShipmentTrace t : adapterTraces) {
                t.setId(null);
                shipmentTraceMapper.insert(t);
            }
            traces = shipmentTraceMapper.selectByShipmentId(shipment.getId());
        }

        List<DeliveryUrge> urges = deliveryUrgeMapper.selectByShipmentId(shipment.getId());

        boolean canUrgeToday = shipment.getStatus() >= 1 && shipment.getStatus() < 4;
        if (canUrgeToday && userId != null) {
            DeliveryUrge today = deliveryUrgeMapper.selectByShipmentAndDate(shipment.getId(), LocalDate.now());
            if (today != null) {
                canUrgeToday = false;
            }
        }

        boolean canReportIssue = false;
        if (shipment.getStatus() == 4 && shipment.getSignedAt() != null) {
            LocalDateTime deadline = shipment.getSignedAt().plusHours(48);
            canReportIssue = LocalDateTime.now().isBefore(deadline);
            List<DeliveryIssue> issues = deliveryIssueMapper.selectByShipmentId(shipment.getId());
            if (!issues.isEmpty()) {
                canReportIssue = false;
            }
        }

        ShipmentDetailVO vo = new ShipmentDetailVO();
        vo.setShipment(shipment);
        vo.setTraces(traces);
        vo.setUrges(urges);
        vo.setCanUrgeToday(canUrgeToday);
        vo.setCanReportIssue(canReportIssue);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public DeliveryUrge urgeDelivery(DeliveryUrgeRequest req, Long userId) {
        Shipment shipment = shipmentMapper.selectById(req.getShipmentId());
        if (shipment == null) {
            throw new IllegalArgumentException("物流记录不存在");
        }
        OrderMain order = orderMainMapper.selectById(shipment.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此订单");
        }
        if (shipment.getStatus() < 1 || shipment.getStatus() >= 4) {
            throw new IllegalArgumentException("当前状态不可催单");
        }
        LocalDate today = LocalDate.now();
        DeliveryUrge existing = deliveryUrgeMapper.selectByShipmentAndDate(req.getShipmentId(), today);
        if (existing != null) {
            throw new IllegalStateException("今日已催单，请耐心等待，每单每日仅限催单1次");
        }

        DeliveryUrge urge = new DeliveryUrge();
        urge.setShipmentId(req.getShipmentId());
        urge.setOrderId(shipment.getOrderId());
        urge.setUserId(userId);
        urge.setUrgeDate(today);
        urge.setRemark(req.getRemark());
        urge.setHandled(0);
        deliveryUrgeMapper.insert(urge);

        log.info("Delivery urged: shipmentId={}, userId={}, date={}", req.getShipmentId(), userId, today);
        return deliveryUrgeMapper.selectById(urge.getId());
    }

    public List<DeliveryUrge> listAllUrges(Integer handled) {
        return deliveryUrgeMapper.selectAll(handled);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleUrge(Long urgeId, Long adminId, String adminRemark) {
        deliveryUrgeMapper.updateHandled(urgeId, 1, adminId, adminRemark);
        log.info("Urge handled: urgeId={}, adminId={}", urgeId, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeliveryIssue createDeliveryIssue(DeliveryIssueCreateRequest req, Long userId) {
        Shipment shipment = shipmentMapper.selectById(req.getShipmentId());
        if (shipment == null) {
            throw new IllegalArgumentException("物流记录不存在");
        }
        OrderMain order = orderMainMapper.selectById(shipment.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此订单");
        }
        if (shipment.getStatus() != 4 || shipment.getSignedAt() == null) {
            throw new IllegalArgumentException("仅已签收订单可标记异常签收");
        }
        LocalDateTime deadline = shipment.getSignedAt().plusHours(48);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException("签收超过48小时，无法标记异常签收");
        }
        List<DeliveryIssue> existing = deliveryIssueMapper.selectByShipmentId(req.getShipmentId());
        if (!existing.isEmpty()) {
            throw new IllegalStateException("该订单已提交异常签收，请等待处理");
        }

        DeliveryIssue issue = new DeliveryIssue();
        issue.setShipmentId(req.getShipmentId());
        issue.setOrderId(shipment.getOrderId());
        issue.setUserId(userId);
        issue.setIssueType(req.getIssueType());
        issue.setDescription(req.getDescription());
        issue.setPhotos(req.getPhotos());
        issue.setStatus(0);
        deliveryIssueMapper.insert(issue);

        shipmentMapper.updateStatus(shipment.getId(), 5);

        log.info("Delivery issue created: shipmentId={}, type={}, userId={}",
                req.getShipmentId(), req.getIssueType(), userId);
        return deliveryIssueMapper.selectById(issue.getId());
    }

    public List<DeliveryIssue> listDeliveryIssuesByOrderId(Long orderId) {
        return deliveryIssueMapper.selectByOrderId(orderId);
    }

    public List<DeliveryIssue> listAllDeliveryIssues(Integer status) {
        return deliveryIssueMapper.selectAll(status);
    }

    public DeliveryIssue getDeliveryIssueById(Long id) {
        return deliveryIssueMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleDeliveryIssue(Long issueId, DeliveryIssueHandleRequest req, Long adminId) {
        DeliveryIssue issue = deliveryIssueMapper.selectById(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("异常签收记录不存在");
        }
        Integer status = req.getStatus();
        if (status == null || (status != 1 && status != 2 && status != 3)) {
            throw new IllegalArgumentException("处理状态无效");
        }
        deliveryIssueMapper.updateStatus(issueId, status, adminId, req.getAdminRemark());
        if (status == 2) {
            Shipment shipment = shipmentMapper.selectById(issue.getShipmentId());
            if (shipment != null && shipment.getStatus() == 5) {
                shipmentMapper.updateStatus(shipment.getId(), 4);
            }
        }
        log.info("Delivery issue handled: issueId={}, status={}, adminId={}", issueId, status, adminId);
    }

    public List<Shipment> listAllShipments() {
        return shipmentMapper.selectAll();
    }

    public ShipmentDetailVO getAdminShipmentDetail(Long shipmentId) {
        return getShipmentDetail(shipmentId);
    }
}
