package com.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class InvoiceCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "发票类型不能为空")
    private String invoiceType;

    @NotBlank(message = "发票抬头不能为空")
    private String title;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String address;

    private String phone;

    @NotBlank(message = "接收邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String receiveEmail;
}
