package com.shop.mapper;

import com.shop.entity.ShipmentTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShipmentTraceMapper {

    int insert(ShipmentTrace trace);

    List<ShipmentTrace> selectByShipmentId(@Param("shipmentId") Long shipmentId);

    int deleteByShipmentId(@Param("shipmentId") Long shipmentId);
}
