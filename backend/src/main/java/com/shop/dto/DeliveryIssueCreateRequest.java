package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryIssueCreateRequest {

    @NotNull(message = "物流ID不能为空")
    private Long shipmentId;

    @NotBlank(message = "问题类型不能为空")
    private String issueType;

    @NotBlank(message = "问题描述不能为空")
    private String description;

    private String photos;
}
