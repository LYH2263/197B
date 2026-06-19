package com.shop.mapper;

import com.shop.entity.SeckillSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillSessionMapper {

    int insert(SeckillSession session);

    SeckillSession selectById(@Param("id") Long id);

    List<SeckillSession> selectAll();

    List<SeckillSession> selectActiveSessions(@Param("now") LocalDateTime now);

    List<SeckillSession> selectUpcomingOrActive(@Param("now") LocalDateTime now);

    int update(SeckillSession session);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    int deleteById(@Param("id") Long id);
}
