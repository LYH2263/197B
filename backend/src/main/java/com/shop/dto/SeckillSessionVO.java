package com.shop.dto;

import com.shop.entity.SeckillSession;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillSessionVO extends SeckillSession {

    private String productName;
    private String productImage;
    private BigDecimal originalPrice;

    private Integer activityStatus;

    private Long remainSeconds;

    private Integer soldPercent;

    private Integer userBoughtCount;
}
