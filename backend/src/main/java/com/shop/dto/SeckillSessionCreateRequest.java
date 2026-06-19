package com.shop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillSessionCreateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价必须大于0")
    private BigDecimal seckillPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer totalStock;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "每人限购不能为空")
    @Min(value = 1, message = "每人限购至少为1")
    private Integer perUserLimit;

    private Integer status;
}
