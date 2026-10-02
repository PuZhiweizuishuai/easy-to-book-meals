package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateSpecGroupDto;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.FoodService;
import com.buguagaoshu.food.service.ProductSkuService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SKU 控制器
 * <p>
 * 调整菜品 SKU 需要登录 + 对应门店 dish:manage 权限
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-08
 */
@RestController
@RequestMapping("/api/sku")
public class ProductSkuController {
    private final ProductSkuService productSkuService;
    private final FoodService foodService;
    private final StoreAuthHelper storeAuthHelper;

    public ProductSkuController(ProductSkuService productSkuService,
                                FoodService foodService,
                                StoreAuthHelper storeAuthHelper) {
        this.productSkuService = productSkuService;
        this.foodService = foodService;
        this.storeAuthHelper = storeAuthHelper;
    }


    @SaCheckLogin
    @PostMapping("adjust")
    public ResponseDetails adjustSkuList(@RequestBody UpdateSpecGroupDto updateSpecGroupDto) {
        if (updateSpecGroupDto.getDishesId() == null) {
            throw new BusinessException(400, "菜品ID不能为空");
        }
        // 通过菜品反查所属门店，校验操作权限
        BaseFood food = foodService.getFood(updateSpecGroupDto.getDishesId());
        if (food.getStorefrontId() == null) {
            throw new BusinessException(400, "该菜品未关联门店，无法调整规格");
        }
        storeAuthHelper.checkStorePermission(food.getStorefrontId(), "dish:manage");
        return ResponseDetails.ok().put("data", productSkuService.adjustSkuList(updateSpecGroupDto));
    }
}
