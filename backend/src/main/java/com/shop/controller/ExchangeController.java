package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ExchangeCreateRequest;
import com.shop.entity.PointsExchangeOrder;
import com.shop.entity.PointsProduct;
import com.shop.service.PointsExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final PointsExchangeService pointsExchangeService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/products")
    public Result<List<PointsProduct>> products() {
        return Result.ok(pointsExchangeService.listOnShelfProducts());
    }

    @GetMapping("/products/{id}")
    public Result<PointsProduct> product(@PathVariable Long id) {
        PointsProduct p = pointsExchangeService.getProduct(id);
        if (p == null) return Result.fail(404, "商品不存在");
        return Result.ok(p);
    }

    @PostMapping("/orders")
    public Result<PointsExchangeOrder> createOrder(Authentication auth,
                                                   @Valid @RequestBody ExchangeCreateRequest req) {
        Long userId = requireUserId(auth);
        PointsExchangeOrder order = pointsExchangeService.createExchangeOrder(userId, req);
        return Result.ok(order);
    }

    @GetMapping("/orders")
    public Result<List<PointsExchangeOrder>> myOrders(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(pointsExchangeService.listMyOrders(userId));
    }

    @GetMapping("/orders/{id}")
    public Result<PointsExchangeOrder> orderDetail(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        PointsExchangeOrder order = pointsExchangeService.getOrderById(id, userId);
        if (order == null) return Result.fail(404, "兑换单不存在");
        return Result.ok(order);
    }

    @PostMapping("/orders/{id}/receive")
    public Result<Void> receive(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        PointsExchangeOrder order = pointsExchangeService.getOrderById(id, userId);
        if (order == null) return Result.fail(404, "兑换单不存在");
        pointsExchangeService.completeOrder(id);
        return Result.ok();
    }
}
