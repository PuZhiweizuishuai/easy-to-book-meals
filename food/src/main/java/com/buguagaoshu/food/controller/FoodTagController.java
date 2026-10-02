package com.buguagaoshu.food.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.FoodTagService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import org.springframework.web.bind.annotation.*;

/**
 * 菜品分类（标签）控制器
 * <p>
 * 查询公开（顾客端菜单分类展示），增删改仅限店主或超级管理员
 * （分类为全局共享数据，禁止普通顾客篡改）
 */
@RestController
@RequestMapping("/api/foodtag")
public class FoodTagController {
    private final FoodTagService foodTagService;
    private final StoreAuthHelper storeAuthHelper;

    public FoodTagController(FoodTagService foodTagService,
                             StoreAuthHelper storeAuthHelper) {
        this.foodTagService = foodTagService;
        this.storeAuthHelper = storeAuthHelper;
    }

    @GetMapping("/map")
    public ResponseDetails getFoodTagMap() {
        return ResponseDetails.ok().put("data", CommonlyUsedDataCache.foodTagMap);
    }

    @GetMapping("/list")
    public ResponseDetails getFoodTagList() {
        return ResponseDetails.ok().put("data", CommonlyUsedDataCache.foodTagList);
    }


    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails createFoodTag(@RequestBody FoodTag foodTag) {
        storeAuthHelper.checkAnyStoreOwner();
        return ResponseDetails.ok().put("data", foodTagService.createTag(foodTag));
    }

    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateFoodTag(@RequestBody FoodTag foodTag) {
        storeAuthHelper.checkAnyStoreOwner();
        return ResponseDetails.ok().put("data", foodTagService.updateTag(foodTag));
    }

    @SaCheckLogin
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteTag(@PathVariable Long id) {
        storeAuthHelper.checkAnyStoreOwner();
        return ResponseDetails.ok().put("data", foodTagService.deleteTag(id));
    }
}
