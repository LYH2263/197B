package com.shop.mapper;

import com.shop.entity.OrderPromotionSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderPromotionSnapshotMapper {

    int insert(OrderPromotionSnapshot snapshot);

    OrderPromotionSnapshot selectByOrderId(@Param("orderId") Long orderId);
}
