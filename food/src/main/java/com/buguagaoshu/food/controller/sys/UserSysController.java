package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.NotificationData;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.StaffInviteDto;
import com.buguagaoshu.food.service.NotificationService;
import com.buguagaoshu.food.service.UserDataService;
import com.buguagaoshu.food.service.UserRoleDataService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import com.buguagaoshu.food.valid.ListValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    private final NotificationService notificationService;

    public UserSysController(UserDataService userDataService,
                             UserRoleDataService userRoleDataService,
                             StoreAuthHelper storeAuthHelper,
                             NotificationService notificationService) {
        this.userDataService = userDataService;
        this.userRoleDataService = userRoleDataService;
        this.storeAuthHelper = storeAuthHelper;
        this.notificationService = notificationService;
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
     * 操作人取当前登录用户，不信任前端传入
     */
    @SaCheckLogin
    @PostMapping("/store/role/assign")
    public ResponseDetails assignStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId) {
        storeAuthHelper.checkStoreOwner(storeId);
        UserRoleData userRoleData = userDataService.assignStoreRole(
                storeId, userId, roleId, StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok().put("data", userRoleData);
    }

    /**
     * 移除用户的门店角色
     */
    @SaCheckLogin
    @PostMapping("/store/role/remove")
    public ResponseDetails removeStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId) {
        storeAuthHelper.checkStoreOwner(storeId);
        userDataService.removeRole(userId, roleId, storeId);
        return ResponseDetails.ok().put("message", "门店角色移除成功");
    }

    /**
     * 查询门店员工列表（含用户信息与角色信息，仅本店店主或超级管理员可查看）
     * <p>
     * 按员工聚合返回：每人一行，含已生效角色 roles、待确认角色 pendingRoles。
     */
    @SaCheckLogin
    @GetMapping("/store/role/list")
    public ResponseDetails listStoreEmployees(@RequestParam Long storeId) {
        storeAuthHelper.checkStoreOwner(storeId);
        return ResponseDetails.ok().put("data", userDataService.listStoreEmployees(storeId));
    }

    // ==================== 员工邀请（按手机号，需员工确认） ====================

    /**
     * 按手机号邀请员工加入门店（可一次邀请多个角色）
     * <p>
     * 邀请不会立即赋权：系统会给该手机号对应的用户发一条待确认通知，
     * 员工在通知中心点「同意」后才真正写入角色绑定；未确认前在员工列表显示「待确认」。
     */
    @SaCheckLogin
    @PostMapping("/store/staff/invite")
    public ResponseDetails inviteStaff(@Valid @RequestBody StaffInviteDto dto) {
        storeAuthHelper.checkStoreOwner(dto.getStoreId());
        NotificationData notification = notificationService.inviteStaff(
                dto.getStoreId(), dto.getPhone(), dto.getRoleIds(), StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok()
                .put("data", notification)
                .put("message", "邀请已发送，等待员工确认");
    }

    /**
     * 撤销待确认的员工邀请
     */
    @SaCheckLogin
    @PostMapping("/store/staff/invite/cancel")
    public ResponseDetails cancelStaffInvite(@RequestParam Long storeId,
                                             @RequestParam Long notificationId) {
        storeAuthHelper.checkStoreOwner(storeId);
        notificationService.cancelStaffInvite(notificationId, storeId, StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok().put("message", "邀请已撤销");
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

    /**
     * 管理员重置指定用户的登录密码（无需原密码）
     * <p>
     * 走专用接口仅更新密码字段，不会清空该用户的昵称、头像、手机号、邮箱等资料。
     */
    @SaCheckPermission("user:manage")
    @PostMapping("/password/reset")
    @Validated
    public ResponseDetails resetPassword(@RequestParam Long userId,
                                         @RequestParam
                                         @Size(min = 6, max = 32, message = "新密码长度需在 6-32 位之间")
                                         String newPassword) {
        userDataService.adminResetPassword(userId, newPassword);
        return ResponseDetails.ok("密码重置成功");
    }
}
