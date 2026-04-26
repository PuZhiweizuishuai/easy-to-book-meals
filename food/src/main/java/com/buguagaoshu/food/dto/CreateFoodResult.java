package com.buguagaoshu.food.dto;

import com.buguagaoshu.food.data.BaseFood;
import lombok.Data;

import java.util.List;

@Data
public class CreateFoodResult {

    private BaseFood food;

    private List<SkuCombination> combinations;
}
