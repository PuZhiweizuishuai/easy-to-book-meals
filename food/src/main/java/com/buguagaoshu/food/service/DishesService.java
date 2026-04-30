package com.buguagaoshu.food.service;


import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.CreateDishesResult;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    private final ProductSkuService productSkuService;

    private final FoodWithTagService foodWithTagService;


    public DishesService(FoodService foodService,
                         SpecGroupService specGroupService,
                         ProductSkuService productSkuService,
                         FoodWithTagService foodWithTagService) {
        this.foodService = foodService;
        this.specGroupService = specGroupService;
        this.productSkuService = productSkuService;
        this.foodWithTagService = foodWithTagService;
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
        BaseFood baseFood = foodService.saveFood(createDishes);

        // 2. 创建 SpecGroup 并保存
        List<SpecGroup> specGroups = specGroupService.createSpecGroups(createDishes, baseFood);


        // 4. 生成 SKU 组合
        List<ProductSku> skuList = productSkuService.createSku(specGroups, baseFood);

        // 5. 批量保存 SKU
        if (!skuList.isEmpty()) {
            productSkuService.saveAllProductSku(skuList);
        }

        // 6. 构建返回结果
        CreateDishesResult result = new CreateDishesResult();
        result.setFood(baseFood);
        result.setSkuList(skuList);
        result.setSpecGroups(specGroups);
        return result;
    }


    /**
     * 获取菜品列表
     * */
    public Page<BaseFood> findBaseFoodList(Integer page, Integer size, String key) {
        return foodService.findAll(page, size, key);
    }

    /**
     * 获取该菜品的菜品信息与所有 SKU
     */
    public CreateDishesResult getDishes(Long id) {
        CreateDishesResult result = new CreateDishesResult();
        BaseFood baseFood = foodService.getFood(id);

        // 获取分类信息
        baseFood.setCategoryIds(foodWithTagService.getFoodTag(baseFood.getId()));
        result.setFood(baseFood);
        // 从数据库中获取该菜品的所有 SKU
        List<ProductSku> skuList = productSkuService.findByDishId(id);
        result.setSkuList(skuList);
        // 从数据库中获取该菜品的所有规格组
        List<SpecGroup> specGroups = specGroupService.getSpecGroupsByDishId(id);
        result.setSpecGroups(specGroups);
        return result;
    }

}
