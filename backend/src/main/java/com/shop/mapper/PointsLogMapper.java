package com.shop.mapper;

import com.shop.entity.PointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsLogMapper {

    int insert(PointsLog pointsLog);

    List<PointsLog> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    int countByUserId(@Param("userId") Long userId);
}
