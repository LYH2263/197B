package com.shop.mapper;

import com.shop.entity.PointsExchangeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsExchangeOrderMapper {

    int insert(PointsExchangeOrder order);

    PointsExchangeOrder selectById(@Param("id") Long id);

    PointsExchangeOrder selectByExchangeNo(@Param("exchangeNo") String exchangeNo);

    List<PointsExchangeOrder> selectByUserId(@Param("userId") Long userId);

    List<PointsExchangeOrder> selectAll();

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateExpress(@Param("id") Long id, @Param("company") String company, @Param("no") String no);
}
