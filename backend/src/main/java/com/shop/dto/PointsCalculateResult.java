package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PointsCalculateResult {
    private Integer availablePoints;
    private Integer maxPointsUsable;
    private BigDecimal maxDiscountAmount;
    private Integer level;
    private BigDecimal pointRate;
}
