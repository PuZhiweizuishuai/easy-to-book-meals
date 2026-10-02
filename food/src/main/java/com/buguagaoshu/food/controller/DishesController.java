package com.buguagaoshu.food.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.CreateDishesResult;
import com.buguagaoshu.food.service.FoodService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.DishesService;


/**
 * 菜品控制器
 * <p>
 * 权限策略：
 * - 菜品详情：公开（顾客端浏览菜品详情）
 * - 门店菜单：公开（见 StoreController /api/store/{id}/dishes）
 * - 菜品管理列表：需登录 + 门店 dish:view 权限
 * - 新增/修改菜品：需登录 + 门店 dish:manage 权限
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@RestController
@RequestMapping("/api/dishes")
@Validated
public class DishesController {
    private final DishesService dishesService;
    private final FoodService foodService;
    private final StoreAuthHelper storeAuthHelper;

    public DishesController(DishesService dishesService,
                            FoodService foodService,
                            StoreAuthHelper storeAuthHelper) {
        this.dishesService = dishesService;
        this.foodService = foodService;
        this.storeAuthHelper = storeAuthHelper;
    }


    /**
     * 获取该菜品的菜品信息与所有 SKU（公开，顾客端菜品详情）
     */
    @GetMapping("/{id}")
    public ResponseDetails getDishes(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", dishesService.getDishes(id));
    }


    /**
     * 店主/店员端分页查询本店菜品（含下架菜品）
     */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails list(@RequestParam @NotNull(message = "门店ID不能为空") Long storefrontId,
                                @RequestParam(value = "page", required = false, defaultValue = "1")
                                @Min(value = 1, message = "页码不能小于1")
                                Integer page,

                                @RequestParam(value = "size", required = false, defaultValue = "10")
                                @Min(value = 1, message = "每页条数不能小于1")
                                @Max(value = 100, message = "每页条数不能超过100")
                                Integer size,

                                @RequestParam(value = "key", required = false)
                                    String key) {
        storeAuthHelper.checkStorePermission(storefrontId, "dish:view");
        return ResponseDetails.ok().put("data", foodService.findByStore(storefrontId, page, size, key));
    }


    /**
     * 增加菜品
     * */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails saveDishes(@Valid @RequestBody CreateDishes createDishes) {
        storeAuthHelper.checkStorePermission(createDishes.getStorefrontId(), "dish:manage");
        return ResponseDetails.ok().put("data", dishesService.createDishes(createDishes));
    }

    /**
     * 更新菜品
     * */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateDishes(@Valid @RequestBody CreateDishesResult createDishesResult) {
        if (createDishesResult.getFood() == null || createDishesResult.getFood().getStorefrontId() == null) {
            throw new IllegalArgumentException("菜品门店信息不能为空");
        }
        storeAuthHelper.checkStorePermission(createDishesResult.getFood().getStorefrontId(), "dish:manage");
        return ResponseDetails.ok().put("data", dishesService.updateDishes(createDishesResult));
    }
}
