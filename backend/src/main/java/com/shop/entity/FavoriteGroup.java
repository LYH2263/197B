package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FavoriteGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
