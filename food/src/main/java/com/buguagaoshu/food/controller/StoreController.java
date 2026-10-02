package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.StoreStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.FoodService;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.service.StoreWithTagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店面控制器（用户端只读）
 * <p>
 * 仅提供面向普通用户的店面浏览和查询功能
 * 店主端操作见 StoreManageController，超管端操作见 StoreSysController
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;
    private final StoreWithTagService storeWithTagService;
    private final FoodService foodService;

    public StoreController(StoreService storeService,
                           StoreWithTagService storeWithTagService,
                           FoodService foodService) {
        this.storeService = storeService;
        this.storeWithTagService = storeWithTagService;
        this.foodService = foodService;
    }

    /**
     * 根据ID查询店面详情
     */
    @GetMapping("/{id}")
    public ResponseDetails getStore(@PathVariable Long id) {
        Store store = storeService.getStore(id);
        store.setTags(storeWithTagService.getStoreTags(id));
        return ResponseDetails.ok().put("data", store);
    }

    /**
     * 查询门店菜单（公开接口，仅营业中/休息中门店可查看，仅返回上架菜品）
     * 顾客端点餐页面使用
     */
    @GetMapping("/{id}/dishes")
    public ResponseDetails getStoreMenu(@PathVariable Long id) {
        Store store = storeService.getStore(id);
        // 待审核与已停业的门店不对外展示菜单
        if (store.getStatus().equals(StoreStatus.PENDING_REVIEW.getCode())
                || store.getStatus().equals(StoreStatus.DISABLED.getCode())) {
            throw new BusinessException(404, "门店不存在或已停业");
        }
        List<BaseFood> menu = foodService.findStoreMenu(id);
        return ResponseDetails.ok().put("data", menu);
    }

    /**
     * 用户端分页查询店面列表，不显示待审核、已停业的店面
     */
    @GetMapping("/list")
    @Validated
    public ResponseDetails list(@RequestParam(value = "page", required = false, defaultValue = "1")
                                @Min(value = 1, message = "页码不能小于1")
                                Integer page,
                                @RequestParam(value = "size", required = false, defaultValue = "10")
                                @Min(value = 1, message = "每页条数不能小于1")
                                @Max(value = 100, message = "每页条数不能超过100")
                                Integer size,
                                @RequestParam(value = "key", required = false)
                                String key) {
        return ResponseDetails.ok().put("data", storeService.findStoresForUser(page, size, key));
    }
}
