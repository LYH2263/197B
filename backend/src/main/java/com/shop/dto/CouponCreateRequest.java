package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCreateRequest {

    @NotBlank(message = "券名称不能为空")
    private String name;

    @NotNull(message = "券类型不能为空")
    private Integer type;

    @NotNull(message = "使用门槛不能为空")
    private BigDecimal threshold;

    private BigDecimal faceValue;

    private BigDecimal discountRate;

    @NotNull(message = "有效期开始不能为空")
    private LocalDateTime validStart;

    @NotNull(message = "有效期结束不能为空")
    private LocalDateTime validEnd;

    @NotNull(message = "发放总量不能为空")
    private Integer totalQuantity;

    @NotNull(message = "每人限领不能为空")
    private Integer perUserLimit;

    private Long applicableCategory;
}
