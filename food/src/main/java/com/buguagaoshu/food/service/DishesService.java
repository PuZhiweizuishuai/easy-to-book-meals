package com.buguagaoshu.food.service;


import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.CreateDishesResult;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    private final ProductSkuService productSkuService;

    private final FoodWithTagService foodWithTagService;
    
    private final FileDataService fileDataService;


    public DishesService(FoodService foodService,
                         SpecGroupService specGroupService,
                         ProductSkuService productSkuService,
                         FoodWithTagService foodWithTagService, 
                         FileDataService fileDataService) {
        this.foodService = foodService;
        this.specGroupService = specGroupService;
        this.productSkuService = productSkuService;
        this.foodWithTagService = foodWithTagService;
        this.fileDataService = fileDataService;
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

        // 6、处理图片信息
        List<FileData> fileDataList = fileDataService.associateFiles(
                createDishes.getFileDataList(),
                baseFood.getId(),
                WebConstant.USE_FILE,
                FileExternalTypeEnum.FOOD
        );

        // 7. 构建返回结果
        CreateDishesResult result = new CreateDishesResult();
        result.setFood(baseFood);
        result.setSkuList(skuList);
        result.setSpecGroups(specGroups);
        result.setFileDataList(fileDataList);
        return result;
    }


    /**
     * 更新菜品，逻辑如下：
     * 1、在 foodService 中创建 updateFood 方法
     *    updateFood 方法应该首先查询需要修改的食物是否存在，存在才进行下一步
     *               创建新的 food 对象，复制用户要修改的属性，避免对如 allSales、updateTime这种由系统生成的数据被用户更改
     *               处理分类变更 分类变更应该由 foodWithTagService 维护
     *               保存数据变更
     *               返回变更后的数据
     * 2、暂时忽略对 SpecGroup 与 SpecValue 的变更
     *               由于 sku 的生成与 SpecGroup 与 SpecValue 相关
     *               直接变更这两属性可能造成 sku 列表数据异常，计划单独实现修改 SpecGroup 与 SpecValue 变更的方法
     *               以便降低系统复杂度
     * 3、在 productSkuService 方法中创建 updateSku方法，以便对用户上传的 sku 数据进行变更
     *                         updateSku 变更方法应该与 updateFood 方法相同，都应该先校验该sku是否存在
     *                         避免变更由系统维护的属性
     *                         由于sku应该是一个列表
     *                         保存变更应该批量保存、减少对数据库的读写
     *                         最后返回修改后的sku
     *
     *  注意：更新数据方法不应该删除原有数据，而是在原有数据上进行修改，删除原有数据再创建新的数据的模式，会造成后期订单数据读取异常
     * */
    @Transactional(rollbackFor = Exception.class)
    public CreateDishesResult updateDishes(CreateDishesResult createDishesResult) {
        BaseFood updateFoodData = createDishesResult.getFood();
        List<ProductSku> updateSkuList = createDishesResult.getSkuList();

        // 1. 更新菜品基础信息（包含分类变更处理）
        BaseFood updatedFood = foodService.updateFood(updateFoodData);

        // 2. 暂时忽略对 SpecGroup 与 SpecValue 的变更
        // 由于 SKU 的生成与 SpecGroup、SpecValue 相关，直接变更可能造成 SKU 列表数据异常
        // 计划单独实现修改 SpecGroup 与 SpecValue 的方法，以降低系统复杂度
        List<SpecGroup> specGroups = specGroupService.getSpecGroupsByDishId(updatedFood.getId());

        // 3. 批量更新 SKU 列表（只修改用户可编辑的属性，保护系统维护的属性）
        List<ProductSku> updatedSkuList = new ArrayList<>();
        if (updateSkuList != null && !updateSkuList.isEmpty()) {
            updatedSkuList = productSkuService.updateSku(updateSkuList);
        }

        // 4. 构建返回结果
        CreateDishesResult result = new CreateDishesResult();
        result.setFood(updatedFood);
        result.setSkuList(updatedSkuList);
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
