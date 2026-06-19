package com.shop.dto;

import lombok.Data;

@Data
public class AdminPointsAdjustRequest {
    private Integer changePoints;
    private String remark;
}
