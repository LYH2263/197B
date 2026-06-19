package com.shop.service.tracking;

import com.shop.entity.Shipment;
import com.shop.entity.ShipmentTrace;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于发货时间的模拟物流适配器
 * 根据当前时间与发货时间的间隔，自动生成不同阶段的物流轨迹
 * 也可作为第三方API对接的 Mock 实现
 */
@Component
public class MockTrackingAdapter implements TrackingAdapter {

    private static final String[] CITIES = {"北京市", "上海市", "广州市", "深圳市", "杭州市", "成都市", "武汉市", "西安市"};

    @Override
    public List<ShipmentTrace> fetchTraces(Shipment shipment) {
        List<ShipmentTrace> traces = new ArrayList<>();
        LocalDateTime shippedAt = shipment.getShippedAt();
        LocalDateTime now = LocalDateTime.now();

        long hoursSinceShipped = shippedAt != null
                ? java.time.Duration.between(shippedAt, now).toHours()
                : 0;

        int status;
        if (hoursSinceShipped < 2) {
            status = 0;
        } else if (hoursSinceShipped < 6) {
            status = 1;
        } else if (hoursSinceShipped < 24) {
            status = 2;
        } else if (hoursSinceShipped < 48) {
            status = 3;
        } else {
            status = 4;
        }

        Long shipmentId = shipment.getId();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        traces.add(buildTrace(shipmentId, 0, "仓库",
                "商家已发货，等待快递员揽收", null, null, shippedAt));

        if (status >= 1) {
            LocalDateTime pickupTime = shippedAt.plusHours(2 + random.nextInt(2));
            traces.add(buildTrace(shipmentId, 1, "发货地 转运中心",
                    "快递员已揽收，快件已到达" + CITIES[random.nextInt(CITIES.length)] + "转运中心",
                    "张师傅", "138****" + String.format("%04d", random.nextInt(10000)), pickupTime));
        }

        if (status >= 2) {
            LocalDateTime transitTime = shippedAt.plusHours(8 + random.nextInt(6));
            String transitCity = CITIES[random.nextInt(CITIES.length)];
            traces.add(buildTrace(shipmentId, 2, transitCity + " 转运中心",
                    "快件已到达" + transitCity + "转运中心，正在分拣中转",
                    null, null, transitTime));

            if (hoursSinceShipped > 14) {
                LocalDateTime transit2 = shippedAt.plusHours(14 + random.nextInt(6));
                String nextCity = CITIES[random.nextInt(CITIES.length)];
                traces.add(buildTrace(shipmentId, 2, nextCity + " 转运中心",
                        "快件已从" + transitCity + "发出，下一站" + nextCity,
                        null, null, transit2));
            }
        }

        if (status >= 3) {
            LocalDateTime deliveryTime = shippedAt.plusHours(26 + random.nextInt(10));
            traces.add(buildTrace(shipmentId, 3, "目的地 派送站",
                    "快件正在派送中，快递员正在为您送货",
                    "李师傅", "139****" + String.format("%04d", random.nextInt(10000)), deliveryTime));
        }

        if (status >= 4) {
            LocalDateTime signTime;
            if (shipment.getSignedAt() != null) {
                signTime = shipment.getSignedAt();
            } else {
                signTime = shippedAt.plusHours(50 + random.nextInt(10));
            }
            traces.add(buildTrace(shipmentId, 4, "收件地址",
                    "快件已签收，签收人：本人签收",
                    null, null, signTime));
        }

        traces.sort((a, b) -> b.getTraceTime().compareTo(a.getTraceTime()));
        return traces;
    }

    @Override
    public Integer getCurrentStatus(Shipment shipment) {
        LocalDateTime shippedAt = shipment.getShippedAt();
        if (shippedAt == null) return 0;
        long hoursSinceShipped = java.time.Duration.between(shippedAt, LocalDateTime.now()).toHours();
        if (hoursSinceShipped < 2) return 0;
        if (hoursSinceShipped < 6) return 1;
        if (hoursSinceShipped < 24) return 2;
        if (hoursSinceShipped < 48) return 3;
        return 4;
    }

    private ShipmentTrace buildTrace(Long shipmentId, int status, String location, String description,
                                     String operator, String operatorPhone, LocalDateTime traceTime) {
        ShipmentTrace trace = new ShipmentTrace();
        trace.setShipmentId(shipmentId);
        trace.setStatus(status);
        trace.setLocation(location);
        trace.setDescription(description);
        trace.setOperator(operator);
        trace.setOperatorPhone(operatorPhone);
        trace.setTraceTime(traceTime);
        return trace;
    }
}
