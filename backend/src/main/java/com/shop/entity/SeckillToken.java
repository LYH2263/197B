package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SeckillToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String token;
    private Long sessionId;
    private Long userId;
    private LocalDateTime expireAt;
    private Integer used;
    private LocalDateTime createdAt;
}
