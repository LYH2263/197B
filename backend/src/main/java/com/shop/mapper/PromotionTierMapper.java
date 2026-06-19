package com.shop.mapper;

import com.shop.entity.PromotionTier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromotionTierMapper {

    int insert(PromotionTier tier);

    List<PromotionTier> selectByPromotionId(@Param("promotionId") Long promotionId);

    int deleteByPromotionId(@Param("promotionId") Long promotionId);
}
