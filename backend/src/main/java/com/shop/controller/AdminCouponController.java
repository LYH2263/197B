package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.CouponCreateRequest;
import com.shop.dto.UserCouponVO;
import com.shop.entity.Coupon;
import com.shop.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody CouponCreateRequest req) {
        Coupon coupon = couponService.create(req);
        return Result.ok(coupon);
    }

    @GetMapping
    public Result<List<Coupon>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(couponService.listAll(status));
    }

    @GetMapping("/{id}")
    public Result<Coupon> getById(@PathVariable Long id) {
        Coupon coupon = couponService.getById(id);
        if (coupon == null) {
            return Result.fail(404, "优惠券不存在");
        }
        return Result.ok(coupon);
    }

    @PostMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id) {
        couponService.invalidate(id);
        return Result.ok();
    }

    @GetMapping("/{id}/claims")
    public Result<List<UserCouponVO>> listClaims(@PathVariable Long id) {
        return Result.ok(couponService.listClaimedRecords(id));
    }

    @GetMapping("/claims")
    public Result<List<UserCouponVO>> listAllClaims() {
        return Result.ok(couponService.listClaimedRecords(null));
    }
}
