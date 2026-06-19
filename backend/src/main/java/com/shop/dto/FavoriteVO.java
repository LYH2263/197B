package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long favoriteId;
    private Long productId;
    private Long groupId;
    private String productName;
    private String productImage;
    private Long categoryId;
    private BigDecimal favorPrice;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private Boolean priceDownFlag;
    private Integer stock;
    private Integer status;
    private Boolean offShelf;
    private Boolean hasPriceAlert;
    private BigDecimal alertTargetPrice;
    private LocalDateTime createdAt;
}
