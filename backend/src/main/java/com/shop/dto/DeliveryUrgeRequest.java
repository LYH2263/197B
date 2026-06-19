package com.shop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryUrgeRequest {

    @NotNull(message = "物流ID不能为空")
    private Long shipmentId;

    private String remark;
}
