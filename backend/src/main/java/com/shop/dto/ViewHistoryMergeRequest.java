package com.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ViewHistoryMergeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<LocalHistoryItem> items;

    @Data
    public static class LocalHistoryItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private BigDecimal viewedPrice;
        private LocalDateTime viewedAt;
    }
}
