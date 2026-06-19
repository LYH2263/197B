package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class FavoriteCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer count;
}
