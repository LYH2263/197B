package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DeliveryIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long shipmentId;
    private Long orderId;
    private Long userId;
    private String issueType;
    private String description;
    private String photos;
    private Integer status;
    private Long adminId;
    private String adminRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
