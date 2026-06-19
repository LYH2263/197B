package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 换货补款单实体
 */
@Data
public class AfterSaleSupplement implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String supplementNo;
    private Long afterSaleId;
    private Long userId;
    private BigDecimal priceDiff;
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
