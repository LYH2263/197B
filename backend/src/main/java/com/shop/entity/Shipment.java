package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;
    private String expressCompanyCode;
    private String expressCompanyName;
    private String trackingNo;
    private Integer status;
    private LocalDateTime shippedAt;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
