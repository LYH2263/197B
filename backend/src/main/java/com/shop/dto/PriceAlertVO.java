package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal currentPrice;
    private BigDecimal targetPrice;
    private Integer status;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
}
