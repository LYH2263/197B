package com.shop.service.tracking;

import com.shop.entity.Shipment;
import com.shop.entity.ShipmentTrace;

import java.util.List;

/**
 * 物流轨迹适配器接口
 * 可对接第三方物流API，当前提供基于发货时间的模拟实现
 */
public interface TrackingAdapter {

    /**
     * 根据物流信息获取轨迹列表
     * @param shipment 物流信息
     * @return 轨迹列表（按时间倒序）
     */
    List<ShipmentTrace> fetchTraces(Shipment shipment);

    /**
     * 获取当前物流状态
     * @param shipment 物流信息
     * @return 状态码：0已发货 1已揽收 2运输中 3派送中 4已签收
     */
    Integer getCurrentStatus(Shipment shipment);
}
