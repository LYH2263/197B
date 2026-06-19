package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPromotionSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long promotionId;
    private String promotionName;
    private Long tierId;
    private BigDecimal tierThreshold;
    private BigDecimal tierDiscount;
    private BigDecimal promotionDiscount;
    private Integer scopeType;
    private Long applicableCategory;
    private LocalDateTime createdAt;
}
