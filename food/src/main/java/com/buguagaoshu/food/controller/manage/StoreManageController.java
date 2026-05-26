package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 店面管理控制器（店主端）
 * <p>
 * 提供店主对自己店面的创建、更新、查询等操作
 * 所有写操作均通过 StoreAuthHelper 校验操作人是否为本店店主或超级管理员
 * <p>
 * 权限校验策略：
 * - 创建店面：登录即可，创建后自动成为该店店主
 * - 更新/查询自己的店面：校验 StoreAuthHelper.checkStoreOwner
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@RestController
@RequestMapping("/api/store/manage")
public class StoreManageController {

    private final StoreService storeService;
    private final StoreAuthHelper storeAuthHelper;

    public StoreManageController(StoreService storeService, StoreAuthHelper storeAuthHelper) {
        this.storeService = storeService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 创建店面
     * 登录用户即可创建，创建者自动记录为店主(createUser)
     * 创建后需由超级管理员审核通过才能对外展示
     */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails addStore(@Valid @RequestBody Store store) {
        store.setCreateUser(StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok().put("data", storeService.saveStore(store));
    }

    /**
     * 更新自己的店面信息
     * 仅本店店主或超级管理员可操作
     */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateStore(@Valid @RequestBody Store store) {
        storeAuthHelper.checkStoreOwner(store.getId());
        return ResponseDetails.ok().put("data", storeService.updateStore(store));
    }

    /**
     * 查询自己管理的店面详情
     */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails getMyStore(@PathVariable Long id) {
        storeAuthHelper.checkStoreOwner(id);
        return ResponseDetails.ok().put("data", storeService.getStore(id));
    }

    /**
     * 店主端分页查询自己管理的店面列表
     * 显示当前用户作为店主的所有店面（含待审核）
     */
    @SaCheckLogin
    @GetMapping("/list")
    @Validated
    public ResponseDetails listMyStores(@RequestParam(value = "page", required = false, defaultValue = "1")
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
}
