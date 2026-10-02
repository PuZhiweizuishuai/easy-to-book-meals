package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.StoreStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.StoreService;
import com.buguagaoshu.food.service.StoreTagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 店面系统管理控制器（超管端）
 * <p>
 * 提供超级管理员对店面的审核、删除、标签管理等系统级操作
 * 店主端操作见 StoreManageController，用户端查询见 StoreController
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@RestController
@RequestMapping("/api/sys/store")
public class StoreSysController {

    private final StoreService storeService;
    private final StoreTagService storeTagService;

    public StoreSysController(StoreService storeService, StoreTagService storeTagService) {
        this.storeService = storeService;
        this.storeTagService = storeTagService;
    }

    /**
     * 超管分页查询所有店面（含待审核，可按状态筛选）
     */
    @SaCheckPermission("store:audit")
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
                                String key,
                                @RequestParam(value = "status", required = false)
                                Integer status) {
        return ResponseDetails.ok().put("data", storeService.findStoresForAdmin(page, size, key, status));
    }

    /**
     * 审核店面（修改状态）
     * 仅允许设置为合法的店面状态：OPEN-审核通过营业 / CLOSED-休息中 / DISABLED-停业 / PENDING_REVIEW-重新待审
     */
    @SaCheckPermission("store:audit")
    @PostMapping("/audit")
    public ResponseDetails auditStore(@RequestParam Long id, @RequestParam Integer status) {
        // 校验状态值合法性
        boolean valid = false;
        for (StoreStatus s : StoreStatus.values()) {
            if (s.getCode() == status) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException(400, "无效的店面状态: " + status);
        }

        return ResponseDetails.ok().put("data", storeService.auditStore(id, status));
    }

    /**
     * 删除店面（逻辑删除）
     */
    @SaCheckPermission("store:audit")
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteStore(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeService.deleteStore(id));
    }

    // ==================== 店面标签管理 ====================

    /**
     * 创建店面标签
     */
    @SaCheckPermission("system:config")
    @PostMapping("/tag/add")
    public ResponseDetails createStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.createTag(storeTag));
    }

    /**
     * 更新店面标签
     */
    @SaCheckPermission("system:config")
    @PostMapping("/tag/update")
    public ResponseDetails updateStoreTag(@RequestBody StoreTag storeTag) {
        return ResponseDetails.ok().put("data", storeTagService.updateTag(storeTag));
    }

    /**
     * 删除店面标签
     */
    @SaCheckPermission("system:config")
    @PostMapping("/tag/delete/{id}")
    public ResponseDetails deleteStoreTag(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", storeTagService.deleteTag(id));
    }
}
