package com.shop.mapper;

import com.shop.entity.AfterSaleLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AfterSaleLogMapper {
    int insert(AfterSaleLog log);
    List<AfterSaleLog> selectByAfterSaleId(@Param("afterSaleId") Long afterSaleId);
}
