package com.buguagaoshu.food.controller.sys;

import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.UserDataService;
import com.buguagaoshu.food.service.UserRoleDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统级用户管理控制器
 * 包含用户角色分配、用户状态管理、店面角色管理等系统级操作
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/sys/user")
public class UserSysController {

    private final UserDataService userDataService;
    private final UserRoleDataService userRoleDataService;

    public UserSysController(UserDataService userDataService,
                             UserRoleDataService userRoleDataService) {
        this.userDataService = userDataService;
        this.userRoleDataService = userRoleDataService;
    }

    /**
     * 管理员分页查询用户列表，支持按昵称模糊搜索
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
        return ResponseDetails.ok().put("data", userDataService.findUsersForAdmin(page, size, key));
    }

    /**
     * 为用户分配系统级角色（storeId 为 null）
     */
    @PostMapping("/role/assign")
    public ResponseDetails assignRole(@RequestParam Long userId,
                                      @RequestParam Long roleId,
                                      @RequestParam(required = false) Long operatorId) {
        UserRoleData userRoleData = userDataService.assignRole(userId, roleId, operatorId);
        return ResponseDetails.ok().put("data", userRoleData);
    }

    /**
     * 移除用户角色
     * storeId 不传时移除系统级角色，传值时移除店面级角色
     */
    @PostMapping("/role/remove")
    public ResponseDetails removeRole(@RequestParam Long userId,
                                      @RequestParam Long roleId,
                                      @RequestParam(required = false) Long storeId) {
        userDataService.removeRole(userId, roleId, storeId);
        return ResponseDetails.ok().put("message", "角色移除成功");
    }

    /**
     * 查询用户拥有的角色列表（包含系统级和店面级）
     */
    @GetMapping("/role/{userId}")
    public ResponseDetails getUserRoles(@PathVariable Long userId) {
        List<UserRoleData> userRoles = userRoleDataService.findByUserId(userId);
        return ResponseDetails.ok().put("data", userRoles);
    }

    /**
     * 修改用户状态（启用/禁用）
     */
    @PostMapping("/status")
    public ResponseDetails updateStatus(@RequestParam Long userId,
                                        @RequestParam Integer status) {
        UserData user = userDataService.updateStatus(userId, status);
        user.setPassword(null);
        return ResponseDetails.ok().put("data", user);
    }

    /**
     * 为用户分配店面级角色
     * 操作人(operatorId)必须是该店面店长(STORE_ADMIN)或超级管理员(SUPER_ADMIN)
     */
    @PostMapping("/store/role/assign")
    public ResponseDetails assignStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId,
                                           @RequestParam Long operatorId) {
        UserRoleData userRoleData = userDataService.assignStoreRole(storeId, userId, roleId, operatorId);
        return ResponseDetails.ok().put("data", userRoleData);
    }

    /**
     * 移除用户的店面级角色
     */
    @PostMapping("/store/role/remove")
    public ResponseDetails removeStoreRole(@RequestParam Long storeId,
                                           @RequestParam Long userId,
                                           @RequestParam Long roleId) {
        userDataService.removeRole(userId, roleId, storeId);
        return ResponseDetails.ok().put("message", "店面角色移除成功");
    }

    /**
     * 查询用户在某店面的角色
     */
    @GetMapping("/store/role")
    public ResponseDetails getStoreUserRoles(@RequestParam Long storeId,
                                             @RequestParam Long userId) {
        return ResponseDetails.ok().put("data",
                userRoleDataService.findByStoreIdAndUserId(storeId, userId));
    }
}
