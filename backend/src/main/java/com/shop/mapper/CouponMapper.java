package com.shop.mapper;

import com.shop.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponMapper {

    int insert(Coupon coupon);

    Coupon selectById(@Param("id") Long id);

    List<Coupon> selectList(@Param("status") Integer status);

    List<Coupon> selectAvailable(@Param("now") LocalDateTime now);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int increaseClaimedQuantity(@Param("id") Long id, @Param("version") Integer version);

    int increaseUsedQuantity(@Param("id") Long id);
}
