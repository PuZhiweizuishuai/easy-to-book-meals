package com.buguagaoshu.food.dto;

import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Data
public class CreateDishesResult {
    private BaseFood food;

    private List<ProductSku> skuList;

    private List<SpecGroup> specGroups;

    private List<FileData> fileDataList;
}
