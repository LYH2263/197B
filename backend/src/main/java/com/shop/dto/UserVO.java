package com.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
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
}
