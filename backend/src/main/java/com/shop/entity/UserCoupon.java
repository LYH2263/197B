package com.shop.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long couponId;
    private String code;
    private Integer status;
    private Long usedOrderId;
    private LocalDateTime usedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expiredAt;
}
