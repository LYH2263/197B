package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FavoriteBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> productIds;
    private Long groupId;
}
