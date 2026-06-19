package com.shop.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeckillOrderMapper {

    int countBySessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    int sumQuantityBySessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
