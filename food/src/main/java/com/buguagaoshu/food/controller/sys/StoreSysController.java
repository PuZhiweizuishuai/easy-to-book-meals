package com.buguagaoshu.food.controller.sys;

import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.service.StoreTagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统级权限api
 * */
@RestController
@RequestMapping("/api/sys/store")
public class StoreSysController {
    private final StoreTagService storeTagService;

    private final StoreService storeService;

    public StoreSysController(StoreTagService storeTagService,
                              StoreService storeService) {
        this.storeTagService = storeTagService;
        this.storeService = storeService;
    }

    /**
     * 管理员分页查询店面列表，显示所有店面（包含待审核）
     */
    @GetMapping("/list")
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

    /**
     * 创建店面标签
     */
    @PostMapping("/tag/add")
    public ResponseDetails createStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.createTag(storeTag));
    }

    /**
     * 更新店面标签
     */
    @PostMapping("/tag/update")
    public ResponseDetails updateStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.updateTag(storeTag));
    }

    /**
     * 删除店面标签
     */
    @PostMapping("/tag/delete/{id}")
    public ResponseDetails deleteStoreTag(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeTagService.deleteTag(id));
    }
}
