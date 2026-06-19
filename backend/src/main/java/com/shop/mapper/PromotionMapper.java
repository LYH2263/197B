package com.shop.mapper;

import com.shop.entity.Promotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PromotionMapper {

    int insert(Promotion promotion);

    Promotion selectById(@Param("id") Long id);

    List<Promotion> selectList(@Param("status") Integer status);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateById(Promotion promotion);

    List<Promotion> selectActive(@Param("now") LocalDateTime now);

    Promotion selectActiveByCategory(@Param("now") LocalDateTime now, @Param("categoryId") Long categoryId);
}
