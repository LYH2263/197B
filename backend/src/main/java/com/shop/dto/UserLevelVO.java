package com.shop.dto;

import lombok.Data;

@Data
public class UserLevelVO {
    private Integer level;
    private String levelName;
    private java.math.BigDecimal totalConsume;
    private java.math.BigDecimal nextLevelThreshold;
    private java.math.BigDecimal progress;
    private java.math.BigDecimal pointRate;
    private String nextLevelName;
    private String benefits;
}
