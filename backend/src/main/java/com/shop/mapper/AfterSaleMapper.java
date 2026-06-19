package com.shop.mapper;

import com.shop.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AfterSaleMapper {
    int insert(AfterSale afterSale);
    AfterSale selectById(@Param("id") Long id);
    AfterSale selectByNo(@Param("afterSaleNo") String afterSaleNo);
    List<AfterSale> selectByUserId(@Param("userId") Long userId);
    List<AfterSale> selectAll(@Param("status") Integer status);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updateApprove(@Param("id") Long id, @Param("refundAmount") BigDecimal refundAmount);
    int updateReject(@Param("id") Long id, @Param("rejectReason") String rejectReason);
    int updateReturnShip(@Param("id") Long id, @Param("company") String company, @Param("trackingNo") String trackingNo);
    int updateExchangeShip(@Param("id") Long id, @Param("company") String company, @Param("trackingNo") String trackingNo, @Param("supplementId") Long supplementId);
    int updateSupplementId(@Param("id") Long id, @Param("supplementId") Long supplementId);
    int countByOrderItemId(@Param("orderItemId") Long orderItemId);
}
