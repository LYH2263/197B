package com.shop.mapper;

import com.shop.dto.ViewHistoryTop10VO;
import com.shop.entity.ViewHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ViewHistoryMapper {

    int insert(ViewHistory viewHistory);

    int updateById(ViewHistory viewHistory);

    ViewHistory selectById(@Param("id") Long id);

    ViewHistory selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<ViewHistory> selectByUserIdWithPage(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByUserId(@Param("userId") Long userId);

    List<ViewHistory> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    int deleteById(@Param("userId") Long userId, @Param("id") Long id);

    int deleteBatchByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    int deleteAllByUserId(@Param("userId") Long userId);

    List<ViewHistoryTop10VO> selectTop10Products(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);
}
