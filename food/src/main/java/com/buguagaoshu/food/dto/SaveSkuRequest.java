package com.buguagaoshu.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaveSkuRequest {

    private Long dishId;

    private List<SkuItemInput> skuItems;

    @Data
    public static class SkuItemInput {
        private List<Long> specValueIds;
        private BigDecimal price;
        private Integer stock;
    }
}
