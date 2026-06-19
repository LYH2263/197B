package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer scopeType;
    private Long applicableCategory;
    private List<TierDTO> tiers;

    @Data
    public static class TierDTO implements Serializable {
        private BigDecimal threshold;
        private BigDecimal discount;
    }
}
