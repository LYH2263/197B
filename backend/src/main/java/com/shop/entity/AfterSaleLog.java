package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 售后操作日志实体
 */
@Data
public class AfterSaleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long afterSaleId;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String action;
    private Integer statusFrom;
    private Integer statusTo;
    private String remark;
    private LocalDateTime createdAt;
}
