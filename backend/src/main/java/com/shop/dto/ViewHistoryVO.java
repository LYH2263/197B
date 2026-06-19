package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ViewHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long categoryId;
    private BigDecimal viewedPrice;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private Integer stock;
    private Integer status;
    private Boolean offShelf;
    private LocalDateTime viewedAt;
}
