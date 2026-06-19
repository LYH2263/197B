package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 售后申请请求 DTO
 */
@Data
public class AfterSaleApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderItemId;
    private Integer type;
    private String reason;
    private String description;
    private List<String> voucherImages;
}
