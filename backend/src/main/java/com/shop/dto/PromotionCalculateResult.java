package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionCalculateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long promotionId;
    private String promotionName;
    private Long tierId;
    private BigDecimal tierThreshold;
    private BigDecimal tierDiscount;
    private BigDecimal promotionDiscount;
    private BigDecimal applicableAmount;
    private BigDecimal originalAmount;
    private Integer scopeType;
    private Long applicableCategory;
    private String categoryName;
    private String desc;

    private BigDecimal nextTierThreshold;
    private BigDecimal nextTierDiscount;
    private BigDecimal gapToNextTier;
    private String nextTierDesc;

    private List<PromotionRecommendProductVO> recommendProducts;
}
