package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.StoreTagService;
import org.springframework.web.bind.annotation.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面标签控制器
 */
@RestController
@RequestMapping("/api/storetag")
public class StoreTagController {
    private final StoreTagService storeTagService;

    public StoreTagController(StoreTagService storeTagService) {
        this.storeTagService = storeTagService;
    }

    /**
     * 获取所有店面标签
     */
    @GetMapping("/list")
    public ResponseDetails getStoreTagList() {
        return ResponseDetails.ok().put("data", storeTagService.getAllStoreTags());
    }

    /**
     * 根据ID查询店面标签
     */
    @GetMapping("/{id}")
    public ResponseDetails getStoreTag(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeTagService.getStoreTag(id));
    }

    /**
     * 创建店面标签
     */
    @PostMapping("/add")
    public ResponseDetails createStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.createTag(storeTag));
    }

    /**
     * 更新店面标签
     */
    @PostMapping("/update")
    public ResponseDetails updateStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.updateTag(storeTag));
    }

    /**
     * 删除店面标签
     */
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteStoreTag(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeTagService.deleteTag(id));
    }
}
