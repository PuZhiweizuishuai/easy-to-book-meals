package com.buguagaoshu.food.controller;


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
}
