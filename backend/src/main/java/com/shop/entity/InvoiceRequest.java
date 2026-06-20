package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InvoiceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long userId;
    private String invoiceType;
    private String title;
    private String taxNumber;
    private String bankName;
    private String bankAccount;
    private String address;
    private String phone;
    private String receiveEmail;
    private String invoiceNumber;
    private Integer status;
    private String rejectReason;
    private Long adminId;
    private String adminName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
