package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CouponCalculateResult {

    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Long bestUserCouponId;
    private UserCouponVO bestCoupon;
    private Long selectedUserCouponId;
    private UserCouponVO selectedCoupon;
    private List<UserCouponVO> availableCoupons;
    private List<UserCouponVO> unavailableCoupons;
}
