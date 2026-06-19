package com.shop.mapper;

import com.shop.entity.DeliveryIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryIssueMapper {

    int insert(DeliveryIssue issue);

    int updateById(DeliveryIssue issue);

    DeliveryIssue selectById(@Param("id") Long id);

    List<DeliveryIssue> selectByShipmentId(@Param("shipmentId") Long shipmentId);

    List<DeliveryIssue> selectByOrderId(@Param("orderId") Long orderId);

    List<DeliveryIssue> selectAll(@Param("status") Integer status);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("adminId") Long adminId, @Param("adminRemark") String adminRemark);
}
