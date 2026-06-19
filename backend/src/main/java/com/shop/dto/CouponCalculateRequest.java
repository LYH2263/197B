package com.shop.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CouponCalculateRequest {

    @NotEmpty(message = "商品列表不能为空")
    private List<OrderItemDTO> items;

    private Long userCouponId;

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private Long categoryId;
        private BigDecimal price;
        private Integer quantity;
    }
}
