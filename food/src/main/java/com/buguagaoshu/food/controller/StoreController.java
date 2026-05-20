package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.service.StoreWithTagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面控制器
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
     * 创建店面
     */
    @PostMapping("/add")
    public ResponseDetails addStore(@Valid @RequestBody Store store) {
        return ResponseDetails.ok().put("data", storeService.saveStore(store));
    }

    /**
     * 更新店面
     */
    @PostMapping("/update")
    public ResponseDetails updateStore(@Valid @RequestBody Store store) {
        return ResponseDetails.ok().put("data", storeService.updateStore(store));
    }

    /**
     * 根据ID查询店面
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

    /**
     * 管理员分页查询店面列表，显示所有店面（包含待审核）
     */
    @GetMapping("/admin/list")
    @Validated
    public ResponseDetails adminList(@RequestParam(value = "page", required = false, defaultValue = "1")
                                     @Min(value = 1, message = "页码不能小于1")
                                     Integer page,

                                     @RequestParam(value = "size", required = false, defaultValue = "10")
                                     @Min(value = 1, message = "每页条数不能小于1")
                                     @Max(value = 100, message = "每页条数不能超过100")
                                     Integer size,

                                     @RequestParam(value = "key", required = false)
                                     String key) {
        return ResponseDetails.ok().put("data", storeService.findStoresForAdmin(page, size, key));
    }

    /**
     * 删除店面（逻辑删除）
     */
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteStore(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeService.deleteStore(id));
    }
}
