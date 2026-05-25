package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.service.StoreWithTagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    public StoreController(StoreService storeService, StoreWithTagService storeWithTagService) {
        this.storeService = storeService;
        this.storeWithTagService = storeWithTagService;
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
     * 用户端分页查询店面列表，不显示待审核的店面
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
