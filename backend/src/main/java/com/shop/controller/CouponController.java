package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.CouponCalculateRequest;
import com.shop.dto.CouponCalculateResult;
import com.shop.dto.CouponVO;
import com.shop.dto.UserCouponVO;
import com.shop.entity.UserCoupon;
import com.shop.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    private Long getUserIdOrNull(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/available")
    public Result<List<CouponVO>> listAvailable(Authentication auth) {
        Long userId = getUserIdOrNull(auth);
        return Result.ok(couponService.listAvailableForClaim(userId));
    }

    @PostMapping("/{id}/claim")
    public Result<UserCoupon> claim(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        UserCoupon uc = couponService.claim(userId, id);
        return Result.ok(uc);
    }

    @GetMapping("/my")
    public Result<List<UserCouponVO>> listMy(Authentication auth,
                                             @RequestParam(required = false) Integer status) {
        Long userId = requireUserId(auth);
        return Result.ok(couponService.listMyCoupons(userId, status));
    }

    @PostMapping("/calculate")
    public Result<CouponCalculateResult> calculate(Authentication auth,
                                                   @Valid @RequestBody CouponCalculateRequest req) {
        Long userId = getUserIdOrNull(auth);
        return Result.ok(couponService.calculate(userId, req));
    }

    @GetMapping("/my/{id}")
    public Result<UserCouponVO> getMyCoupon(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        UserCoupon uc = couponService.getUserCouponById(id);
        if (uc == null || !uc.getUserId().equals(userId)) {
            return Result.fail(404, "优惠券不存在");
        }
        List<UserCouponVO> list = couponService.listMyCoupons(userId, null);
        for (UserCouponVO vo : list) {
            if (vo.getId().equals(id)) {
                return Result.ok(vo);
            }
        }
        return Result.fail(404, "优惠券不存在");
    }
}
