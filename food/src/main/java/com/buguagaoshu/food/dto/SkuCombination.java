package com.buguagaoshu.food.dto;

import lombok.Data;

import java.util.List;

@Data
public class SkuCombination {

    private Long dishId;

    private List<Long> specValueIds;

    private List<String> specValueNames;
}
