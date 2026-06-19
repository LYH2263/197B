package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer totalStock;
    private Integer soldStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer perUserLimit;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
