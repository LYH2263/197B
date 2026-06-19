package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String image;
    private String description;
    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
