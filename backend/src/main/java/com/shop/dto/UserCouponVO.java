package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long couponId;
    private String code;
    private Integer status;
    private String statusDesc;
    private Long usedOrderId;
    private LocalDateTime usedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expiredAt;
    private String name;
    private Integer type;
    private String typeDesc;
    private BigDecimal threshold;
    private BigDecimal faceValue;
    private BigDecimal discountRate;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Long applicableCategory;
    private String categoryName;
    private String desc;
    private Boolean available;
    private BigDecimal discountAmount;
}
