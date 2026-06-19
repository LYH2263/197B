package com.shop.mapper;

import com.shop.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShipmentMapper {

    int insert(Shipment shipment);

    int updateById(Shipment shipment);

    Shipment selectById(@Param("id") Long id);

    Shipment selectByOrderId(@Param("orderId") Long orderId);

    Shipment selectByTrackingNo(@Param("trackingNo") String trackingNo);

    List<Shipment> selectAll();

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
