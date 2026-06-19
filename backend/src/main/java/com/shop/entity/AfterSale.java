package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后主表实体
 */
@Data
public class AfterSale implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String afterSaleNo;
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer type;
    private String reason;
    private String description;
    private String voucherImages;
    private BigDecimal originalPrice;
    private Integer originalQuantity;
    private BigDecimal originalTotalAmount;
    private BigDecimal refundAmount;
    private Integer status;
    private String rejectReason;
    private String returnCompany;
    private String returnTrackingNo;
    private String exchangeCompany;
    private String exchangeTrackingNo;
    private Long supplementId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
