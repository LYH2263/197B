package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {

    private Long id;
    private String name;
    private Integer type;
    private String typeDesc;
    private BigDecimal threshold;
    private BigDecimal faceValue;
    private BigDecimal discountRate;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer perUserLimit;
    private Long applicableCategory;
    private String categoryName;
    private Integer userClaimedCount;
    private Boolean canClaim;
    private String desc;
}
