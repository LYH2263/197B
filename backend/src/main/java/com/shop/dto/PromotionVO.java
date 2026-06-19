package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer scopeType;
    private String scopeTypeDesc;
    private Long applicableCategory;
    private String categoryName;
    private Integer status;
    private String statusDesc;
    private List<TierVO> tiers;
    private String tierDesc;
    private LocalDateTime createdAt;

    @Data
    public static class TierVO implements Serializable {
        private Long id;
        private BigDecimal threshold;
        private BigDecimal discount;
    }
}
