package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ViewHistoryTop10VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal currentPrice;
    private Long viewCount;
}
