package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
     * 创建店面（申请开店）
     * 需具备 store:create 权限（顾客注册即拥有），创建者记录为 createUser，
     * 审核通过前不授予店主角色；同一用户存在待审核门店时禁止重复创建
     */
    @SaCheckPermission("store:create")
    @PostMapping("/add")
    public ResponseDetails addStore(@Valid @RequestBody Store store) {
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
     * 分页查询与当前用户相关的店面列表
     * 范围 = 拥有门店角色的店面 ∪ 自己创建的店面（含待审核）；
     * 超级管理员查看全部门店请使用 /api/sys/store/list
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
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data",
                storeService.findMyStores(userId, page, size, key));
    }
}
