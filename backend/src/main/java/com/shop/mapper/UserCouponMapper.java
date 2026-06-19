package com.shop.mapper;

import com.shop.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserCouponMapper {

    int insert(UserCoupon userCoupon);

    UserCoupon selectById(@Param("id") Long id);

    UserCoupon selectByCode(@Param("code") String code);

    List<UserCoupon> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    List<UserCoupon> selectByUserId(@Param("userId") Long userId);

    List<UserCoupon> selectAll(@Param("couponId") Long couponId);

    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId, @Param("usedAt") LocalDateTime usedAt);

    int expireOverdue(@Param("now") LocalDateTime now);

    List<UserCoupon> selectAvailableByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
