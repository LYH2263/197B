package com.shop.dto;

import lombok.Data;

@Data
public class ExpressCompanyVO {

    private String code;

    private String name;

    public ExpressCompanyVO(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
