package com.shop.dto;

import com.shop.entity.OrderItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;
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

    private BigDecimal orderAmount;
    private List<OrderItem> orderItems;
}
