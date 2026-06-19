package com.shop.mapper;

import com.shop.entity.FavoriteGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteGroupMapper {

    int insert(FavoriteGroup group);

    int updateById(FavoriteGroup group);

    FavoriteGroup selectById(@Param("id") Long id);

    FavoriteGroup selectByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    List<FavoriteGroup> selectByUserId(@Param("userId") Long userId);

    int deleteById(@Param("id") Long id);
}
