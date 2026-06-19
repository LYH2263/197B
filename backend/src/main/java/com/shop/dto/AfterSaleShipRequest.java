package com.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 填写物流请求 DTO
 */
@Data
public class AfterSaleShipRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String company;
    private String trackingNo;
}
