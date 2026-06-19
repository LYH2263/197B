package com.shop.mapper;

import com.shop.entity.SeckillToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SeckillTokenMapper {

    int insert(SeckillToken token);

    SeckillToken selectByToken(@Param("token") String token);

    int markUsed(@Param("token") String token);

    int countBySessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    int countValidBySessionIdAndUserId(@Param("sessionId") Long sessionId,
                                        @Param("userId") Long userId,
                                        @Param("now") LocalDateTime now);

    int deleteExpired(@Param("now") LocalDateTime now);
}
