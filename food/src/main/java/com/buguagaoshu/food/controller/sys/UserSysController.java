package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.UserDataService;
import com.buguagaoshu.food.service.UserRoleDataService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import com.buguagaoshu.food.valid.ListValue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统级用户管理控制器（RBAC-D 域感知）
 * <p>
 * 包含用户角色分配、用户状态管理等系统级操作
 * 角色分配遵循域隔离规则：
 * - 全局角色分配：仅超级管理员可操作
 * - 门店角色分配：仅本店店主可操作
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/sys/user")
public class UserSysController {

    private final UserDataService userDataService;
    private final UserRoleDataService userRoleDataService;
    private final StoreAuthHelper storeAuthHelper;

    public UserSysController(UserDataService userDataService,
                             UserRoleDataService userRoleDataService,
                             StoreAuthHelper storeAuthHelper) {
        this.userDataService = userDataService;
        this.userRoleDataService = userRoleDataService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 管理员分页查询用户列表，支持按昵称模糊搜索
     */
    @SaCheckPermission("user:manage")
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
        return ResponseDetails.ok().put("data", userDataService.findUsersForAdmin(page, size, key));
    }

    // ==================== 全局角色分配 ====================

    /**
     * 为用户分配全局角色（仅超级管理员可操作）
     */
    @SaCheckPermission("role:global:manage")
    @PostMapping("/global/role/assign")
    public ResponseDetails assignGlobalRole(@RequestParam Long userId,
                                            @RequestParam Long roleId,
                                            @RequestParam(required = false) Long operatorId) {
        UserRoleData userRoleData = userDataService.assignGlobalRole(userId, roleId, operatorId);
        return ResponseDetails.ok().put("data", userRoleData);
    }

    /**
     * 移除用户的全局角色
     */
    @SaCheckPermission("role:global:manage")
    @PostMapping("/global/role/remove")
    public ResponseDetails removeGlobalRole(@RequestParam Long userId,
                                            @RequestParam Long roleId) {
        userDataService.removeRole(userId, roleId, null);
        return ResponseDetails.ok().put("message", "全局角色移除成功");
    }



    // ==================== 门店角色分配 ====================

    /**
     * 为用户分配门店角色（仅本店店主或超级管理员可操作）
     * TODO 超级管理员可以操作所有店面
     *      本店店主只可以操作自己的
     */
    @SaCheckPermission(value = {"role:global:manage", "role:store:manage"}, mode = SaMode.OR)
    @PostMapping("/store/role/assign")
    public ResponseDetails assignStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId,
                                           @RequestParam Long operatorId) {

        UserRoleData userRoleData = userDataService.assignStoreRole(storeId, userId, roleId, operatorId);
        return ResponseDetails.ok().put("data", userRoleData);
    }

    /**
     * 移除用户的门店角色
     */
    @SaCheckPermission(value = {"role:global:manage", "role:store:manage"}, mode = SaMode.OR)
    @PostMapping("/store/role/remove")
    public ResponseDetails removeStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId) {
        storeAuthHelper.checkStoreOwner(storeId);
        userDataService.removeRole(userId, roleId, storeId);
        return ResponseDetails.ok().put("message", "门店角色移除成功");
    }

    /**
     * 查询用户在某门店的角色
     */
    @SaCheckLogin
    @GetMapping("/store/role")
    public ResponseDetails getStoreUserRoles(@RequestParam Long storeId,
                                             @RequestParam Long userId) {
        return ResponseDetails.ok().put("data",
                userRoleDataService.findByStoreIdAndUserId(storeId, userId));
    }

    /**
     * 查询用户所有角色（全局+门店）
     */
    @SaCheckLogin
    @GetMapping("/role/{userId}")
    public ResponseDetails getUserAllRoles(@PathVariable Long userId) {
        List<UserRoleData> userRoles = userRoleDataService.findByUserId(userId);
        return ResponseDetails.ok().put("data", userRoles);
    }

    // ==================== 用户状态管理 ====================

    /**
     * 修改用户状态（启用/禁用）
     */
    @SaCheckPermission("user:manage")
    @PostMapping("/status")
    @Validated
    public ResponseDetails updateStatus(@RequestParam Long userId,
                                        @RequestParam @ListValue(value = {0, 1}) Integer status) {
        UserData user = userDataService.updateStatus(userId, status);
        user.setPassword(null);
        return ResponseDetails.ok().put("data", user);
    }
}
