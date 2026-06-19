package com.shop.mapper;

import com.shop.entity.PriceAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PriceAlertMapper {

    int insert(PriceAlert alert);

    int updateById(PriceAlert alert);

    PriceAlert selectById(@Param("id") Long id);

    PriceAlert selectActiveByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<PriceAlert> selectByUserId(@Param("userId") Long userId);

    List<PriceAlert> selectAllActive();

    int cancelByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int triggerById(@Param("id") Long id);
}
