package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderMain implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private String orderType;
    private Long seckillSessionId;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private Long couponId;
    private BigDecimal pointsDiscount;
    private Integer pointsUsed;
    private Integer pointsEarned;
    private Integer status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
