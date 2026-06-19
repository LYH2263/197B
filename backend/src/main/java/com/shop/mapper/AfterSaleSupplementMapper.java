package com.shop.mapper;

import com.shop.entity.AfterSaleSupplement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AfterSaleSupplementMapper {
    int insert(AfterSaleSupplement supplement);
    AfterSaleSupplement selectById(@Param("id") Long id);
    AfterSaleSupplement selectByNo(@Param("supplementNo") String supplementNo);
    AfterSaleSupplement selectByAfterSaleId(@Param("afterSaleId") Long afterSaleId);
    List<AfterSaleSupplement> selectByUserId(@Param("userId") Long userId);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("paidAt") java.time.LocalDateTime paidAt);
}
