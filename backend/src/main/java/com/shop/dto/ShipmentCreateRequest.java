package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipmentCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "快递公司编码不能为空")
    private String expressCompanyCode;

    @NotBlank(message = "快递公司名称不能为空")
    private String expressCompanyName;

    @NotBlank(message = "运单号不能为空")
    private String trackingNo;
}
