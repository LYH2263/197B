package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private String role;
    private Integer points;
    private BigDecimal totalConsume;
    private Integer level;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
