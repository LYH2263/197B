package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsExchangeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String exchangeNo;
    private Long userId;
    private Long pointsProductId;
    private String productName;
    private String productImage;
    private Integer pointsCost;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Integer status;
    private String expressCompany;
    private String expressNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
