package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.DeliveryIssueCreateRequest;
import com.shop.dto.DeliveryUrgeRequest;
import com.shop.dto.ShipmentDetailVO;
import com.shop.entity.DeliveryIssue;
import com.shop.entity.DeliveryUrge;
import com.shop.entity.OrderMain;
import com.shop.entity.Shipment;
import com.shop.mapper.OrderMainMapper;
import com.shop.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final OrderMainMapper orderMainMapper;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/order/{orderId}")
    public Result<ShipmentDetailVO> getByOrderId(Authentication auth, @PathVariable Long orderId) {
        Long userId = requireUserId(auth);
        ShipmentDetailVO vo = shipmentService.getShipmentDetailByOrderId(orderId, userId);
        if (vo == null) {
            return Result.fail(404, "该订单暂无物流信息");
        }
        return Result.ok(vo);
    }

    @GetMapping("/{id}")
    public Result<ShipmentDetailVO> getById(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        ShipmentDetailVO vo = shipmentService.getShipmentDetail(id);
        if (vo == null) {
            return Result.fail(404, "物流信息不存在");
        }
        Shipment shipment = vo.getShipment();
        if (shipment != null) {
            OrderMain order = orderMainMapper.selectById(shipment.getOrderId());
            if (order == null || !order.getUserId().equals(userId)) {
                return Result.fail(403, "无权查看此物流信息");
            }
        }
        return Result.ok(vo);
    }

    @PostMapping("/urge")
    public Result<DeliveryUrge> urge(Authentication auth, @Valid @RequestBody DeliveryUrgeRequest req) {
        Long userId = requireUserId(auth);
        DeliveryUrge urge = shipmentService.urgeDelivery(req, userId);
        return Result.ok(urge);
    }

    @PostMapping("/issue")
    public Result<DeliveryIssue> reportIssue(Authentication auth, @Valid @RequestBody DeliveryIssueCreateRequest req) {
        Long userId = requireUserId(auth);
        DeliveryIssue issue = shipmentService.createDeliveryIssue(req, userId);
        return Result.ok(issue);
    }

    @GetMapping("/order/{orderId}/issues")
    public Result<List<DeliveryIssue>> listIssuesByOrder(Authentication auth, @PathVariable Long orderId) {
        requireUserId(auth);
        return Result.ok(shipmentService.listDeliveryIssuesByOrderId(orderId));
    }
}
