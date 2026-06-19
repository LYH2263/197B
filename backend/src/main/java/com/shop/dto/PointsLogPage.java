package com.shop.dto;

import com.shop.entity.PointsLog;
import lombok.Data;

import java.util.List;

@Data
public class PointsLogPage {
    private List<PointsLog> list;
    private int total;
    private int page;
    private int size;
}
