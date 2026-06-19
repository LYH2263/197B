package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PromotionRecommendProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
}
