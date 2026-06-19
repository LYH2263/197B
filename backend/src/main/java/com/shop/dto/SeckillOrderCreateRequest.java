package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillOrderCreateRequest {

    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    @NotBlank(message = "秒杀token不能为空")
    private String token;

    @NotNull(message = "收货人不能为空")
    private String receiverName;

    @NotNull(message = "收货电话不能为空")
    private String receiverPhone;

    @NotNull(message = "收货地址不能为空")
    private String receiverAddress;
}
