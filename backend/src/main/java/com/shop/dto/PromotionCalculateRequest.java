package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PromotionCalculateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO implements Serializable {
        private Long productId;
        private Long categoryId;
        private BigDecimal price;
        private Integer quantity;
    }
}
