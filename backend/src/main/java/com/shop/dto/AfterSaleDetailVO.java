package com.shop.dto;

import com.shop.entity.AfterSale;
import com.shop.entity.AfterSaleLog;
import com.shop.entity.AfterSaleSupplement;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 售后详情 VO
 */
@Data
public class AfterSaleDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private AfterSale afterSale;
    private List<AfterSaleLog> logs;
    private AfterSaleSupplement supplement;
}
