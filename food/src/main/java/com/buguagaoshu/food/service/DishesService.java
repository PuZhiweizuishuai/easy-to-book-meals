package com.buguagaoshu.food.service;


import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.CreateDishesResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 菜品服务
 * 负责构建前端需要的最终数据
 * 保存结构化数据
 */
@Service
public class DishesService {
    private final FoodService foodService;

    private final SpecGroupService specGroupService;

    private final SpecValueService specValueService;

    private final ProductSkuService productSkuService;


    public DishesService(FoodService foodService,
                         SpecGroupService specGroupService,
                         SpecValueService specValueService,
                         ProductSkuService productSkuService) {
        this.foodService = foodService;
        this.specGroupService = specGroupService;
        this.specValueService = specValueService;
        this.productSkuService = productSkuService;
    }

    /**
     * 创建菜品
     * 逻辑如下：
     * 1、通过 CreateDishes 类中的属性创建 BaseFood，将 BaseFood 写入到 foodService
     * 2、然后读取 CreateDishes 中数据创建规格组 SpecGroup 保存
     * 3、读取 SpecGroup 中的 value 值创建 SpecValue 保存
     * 4、生成 SKU，系统根据商家选择的规格值，通过笛卡尔积算法自动生成所有可能的组合
     * 5、系统初始价格与库存均为 0
     * 6、最后，将这些组合信息批量插入到 ProductSku 中保存
     * */
    @Transactional(rollbackFor = Exception.class)
    public CreateDishesResult createDishes(CreateDishes createDishes) {
        // 1. 创建 BaseFood 并保存
        BaseFood baseFood = new BaseFood();
        baseFood.setName(createDishes.getName());
        baseFood.setDescription(createDishes.getDescription());
        baseFood.setTopImage(createDishes.getTopImage());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            baseFood.setImageList(objectMapper.writeValueAsString(createDishes.getImageList()));
        } catch (Exception e) {
            baseFood.setImageList("[]");
        }
        baseFood.setPriceType(createDishes.getPriceType());
        baseFood.setCategoryIds(createDishes.getCategoryIds());
        baseFood.setStock(createDishes.getStock());
        baseFood.setStatus(createDishes.getStatus());
        baseFood = foodService.saveFood(baseFood);

        // 2. 创建 SpecGroup 并保存
        List<SpecGroup> specGroups = createDishes.getSpecGroups();
        List<Long> specValueIds = new ArrayList<>();
        if (specGroups != null && !specGroups.isEmpty()) {
            for (SpecGroup specGroup : specGroups) {
                specGroup.setDishId(baseFood.getId());
                specGroupService.saveSpecGroup(specGroup);
                
                // 3. 创建 SpecValue 并保存
                List<SpecValue> specValues = specGroup.getValues();
                if (specValues != null && !specValues.isEmpty()) {
                    for (SpecValue specValue : specValues) {
                        specValue.setGroupId(specGroup.getId());
                        specValueService.saveSpecValue(specValue);
                        specValueIds.add(specValue.getId());
                    }
                }
            }
        }

        // 4. 生成 SKU 组合
        List<ProductSku> skuList = new ArrayList<>();
        if (specGroups != null && !specGroups.isEmpty()) {
            // 准备规格值列表，按规格组分组
            List<List<Long>> specValueGroups = new ArrayList<>();
            for (SpecGroup specGroup : specGroups) {
                List<SpecValue> values = specGroup.getValues();
                if (values != null && !values.isEmpty()) {
                    List<Long> groupValues = new ArrayList<>();
                    for (SpecValue value : values) {
                        groupValues.add(value.getId());
                    }
                    specValueGroups.add(groupValues);
                }
            }
            
            // 生成笛卡尔积
            List<List<Long>> combinations = cartesianProduct(specValueGroups);
            
            // 创建 SKU
            for (List<Long> combination : combinations) {
                ProductSku sku = new ProductSku();
                sku.setDishId(baseFood.getId());
                sku.setUnitPrice(BigDecimal.ZERO);
                sku.setUnit("份");
                sku.setStock(0);
                sku.setDiscount(1.0);
                sku.setDiscountEndTime(0L);
                try {
                    sku.setSpecs(objectMapper.writeValueAsString(combination));
                } catch (Exception e) {
                    sku.setSpecs("[]");
                }
                sku.setStatus(WebConstant.ENABLE);
                skuList.add(sku);
            }
        }
        
        // 5. 批量保存 SKU
        if (!skuList.isEmpty()) {
            productSkuService.saveAllProductSku(skuList);
        }

        // 6. 构建返回结果
        CreateDishesResult result = new CreateDishesResult();
        result.setFood(baseFood);
        result.setSkuList(skuList);
        return result;
    }
    
    /**
     * 计算笛卡尔积
     */
    private List<List<Long>> cartesianProduct(List<List<Long>> lists) {
        List<List<Long>> result = new ArrayList<>();
        if (lists.isEmpty()) {
            return result;
        }
        
        // 初始化结果列表
        for (Long value : lists.get(0)) {
            List<Long> list = new ArrayList<>();
            list.add(value);
            result.add(list);
        }
        
        // 逐步计算笛卡尔积
        for (int i = 1; i < lists.size(); i++) {
            List<List<Long>> newResult = new ArrayList<>();
            List<Long> currentList = lists.get(i);
            
            for (List<Long> existingList : result) {
                for (Long value : currentList) {
                    List<Long> newList = new ArrayList<>(existingList);
                    newList.add(value);
                    newResult.add(newList);
                }
            }
            
            result = newResult;
        }
        
        return result;
    }
}
