package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionTier implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long promotionId;
    private BigDecimal threshold;
    private BigDecimal discount;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
