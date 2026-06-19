package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Promotion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer scopeType;
    private Long applicableCategory;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private transient List<PromotionTier> tiers;
}
