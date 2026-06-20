package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class InvoiceReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceNumber;

    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;
}
