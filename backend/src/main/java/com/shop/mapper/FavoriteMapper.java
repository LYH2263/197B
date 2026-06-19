package com.shop.mapper;

import com.shop.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    int insert(Favorite favorite);

    int updateById(Favorite favorite);

    Favorite selectById(@Param("id") Long id);

    Favorite selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<Favorite> selectByUserId(@Param("userId") Long userId);

    List<Favorite> selectByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    int countByUserId(@Param("userId") Long userId);

    int countByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int deleteById(@Param("id") Long id);

    int deleteBatchByUserIdAndProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);

    int updateGroupId(@Param("userId") Long userId, @Param("productId") Long productId, @Param("groupId") Long groupId);

    int updateBatchGroupId(@Param("userId") Long userId, @Param("productIds") List<Long> productIds, @Param("groupId") Long groupId);

    int clearGroupIdByGroupId(@Param("groupId") Long groupId);
}
