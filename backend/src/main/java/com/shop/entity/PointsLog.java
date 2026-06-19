package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Integer type;
    private Integer changePoints;
    private Integer balance;
    private String relatedType;
    private Long relatedId;
    private String remark;
    private LocalDateTime createdAt;
}
