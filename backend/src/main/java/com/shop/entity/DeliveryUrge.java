package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryUrge implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long shipmentId;
    private Long orderId;
    private Long userId;
    private LocalDate urgeDate;
    private String remark;
    private Integer handled;
    private Long adminId;
    private String adminRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
