package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ViewHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal viewedPrice;
    private LocalDateTime viewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
