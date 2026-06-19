package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FavoriteGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer sortOrder;
    private Integer count;
    private LocalDateTime createdAt;
}
