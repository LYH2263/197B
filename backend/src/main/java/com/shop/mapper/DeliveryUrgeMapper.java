package com.shop.mapper;

import com.shop.entity.DeliveryUrge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DeliveryUrgeMapper {

    int insert(DeliveryUrge urge);

    DeliveryUrge selectById(@Param("id") Long id);

    DeliveryUrge selectByShipmentAndDate(@Param("shipmentId") Long shipmentId, @Param("urgeDate") LocalDate urgeDate);

    List<DeliveryUrge> selectByShipmentId(@Param("shipmentId") Long shipmentId);

    List<DeliveryUrge> selectAll(@Param("handled") Integer handled);

    int updateHandled(@Param("id") Long id, @Param("handled") Integer handled,
                      @Param("adminId") Long adminId, @Param("adminRemark") String adminRemark);
}
