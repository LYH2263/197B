package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItemVO> items;
    private BigDecimal totalAmount;
    private Integer checkedCount;

    private PromotionCalculateResult promotion;
}
