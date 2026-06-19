package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 售后审核请求 DTO
 */
@Data
public class AfterSaleReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal refundAmount;
    private String rejectReason;
}
