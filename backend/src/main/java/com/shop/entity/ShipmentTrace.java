package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShipmentTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long shipmentId;
    private Integer status;
    private String location;
    private String description;
    private String operator;
    private String operatorPhone;
    private LocalDateTime traceTime;
    private LocalDateTime createdAt;
}
