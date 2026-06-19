package com.shop.dto;

import com.shop.entity.DeliveryUrge;
import com.shop.entity.Shipment;
import com.shop.entity.ShipmentTrace;
import lombok.Data;

import java.util.List;

@Data
public class ShipmentDetailVO {

    private Shipment shipment;

    private List<ShipmentTrace> traces;

    private List<DeliveryUrge> urges;

    private boolean canUrgeToday;

    private boolean canReportIssue;
}
