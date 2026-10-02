package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.PermissionDataService;
import com.buguagaoshu.food.service.RoleDataService;
import com.buguagaoshu.food.service.RolePermissionDataService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（RBAC-D 域感知）
 * <p>
 * 全局角色管理：仅超级管理员可操作
 * 门店角色管理：仅本店店主可操作
 * <p>
 * 核心隔离规则：
 * 1. 全局角色(domain=GLOBAL)只能绑定全局权限
 * 2. 门店角色(domain=STORE)只能绑定门店权限
 * 3. 两类角色不能交叉绑定、不能跨域授权
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/sys/role")
public class RoleSysController {

    private final RoleDataService roleDataService;
    private final RolePermissionDataService rolePermissionDataService;
    private final PermissionDataService permissionDataService;
    private final StoreAuthHelper storeAuthHelper;

    public RoleSysController(RoleDataService roleDataService,
                             RolePermissionDataService rolePermissionDataService,
                             PermissionDataService permissionDataService,
                             StoreAuthHelper storeAuthHelper) {
        this.roleDataService = roleDataService;
        this.rolePermissionDataService = rolePermissionDataService;
        this.permissionDataService = permissionDataService;
        this.storeAuthHelper = storeAuthHelper;
    }

    // ==================== 聚合查询（带权限详情） ====================

    /**
     * 查询所有角色（附带权限详情列表），从缓存读取
     * 前端可通过此接口一次性获取角色和权限数据，减少请求次数
     */
    @SaCheckPermission("role:global:view")
    @GetMapping("/list-with-permissions")
    public ResponseDetails listWithPermissions() {
        return ResponseDetails.ok().put("data", RolePermissionCache.getAllRolesWithPermissions());
    }

    /**
     * 查询全局角色（附带权限详情列表），从缓存读取
     */
    @SaCheckPermission("role:global:view")
    @GetMapping("/global/list-with-permissions")
    public ResponseDetails listGlobalRolesWithPermissions() {
        return ResponseDetails.ok().put("data", RolePermissionCache.getGlobalRolesWithPermissions());
    }

    /**
     * 查询门店模板角色（附带权限详情列表），从缓存读取
     */
    @GetMapping("/store/template/list-with-permissions")
    public ResponseDetails listStoreTemplateRolesWithPermissions() {
        return ResponseDetails.ok().put("data", RolePermissionCache.getStoreTemplateRolesWithPermissions());
    }

    // ==================== 全局角色管理 ====================


    /**
     * 创建全局角色
     */
    @SaCheckPermission("role:global:manage")
    @PostMapping("/global/add")
    public ResponseDetails addGlobalRole(@RequestBody RoleData roleData) {
        roleData.setDomain(RoleDomainEnum.GLOBAL.getCode());
        roleData.setStoreId(null);
        return ResponseDetails.ok().put("data", roleDataService.save(roleData));
    }

    /**
     * 更新全局角色
     */
    @SaCheckPermission("role:global:manage")
    @PostMapping("/global/update")
    public ResponseDetails updateGlobalRole(@RequestBody RoleData roleData) {
        RoleData existing = roleDataService.findById(roleData.getId())
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        if (!existing.isGlobal()) {
            throw new BusinessException(400, "只能修改全局域角色");
        }
        roleData.setDomain(RoleDomainEnum.GLOBAL.getCode());
        return ResponseDetails.ok().put("data", roleDataService.update(roleData));
    }

    /**
     * 删除全局角色
     */
    @SaCheckPermission("role:global:manage")
    @PostMapping("/global/delete/{id}")
    public ResponseDetails deleteGlobalRole(@PathVariable Long id) {
        RoleData existing = roleDataService.findById(id)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        if (!existing.isGlobal()) {
            throw new BusinessException(400, "只能删除全局域角色");
        }
        roleDataService.deleteById(id);
        return ResponseDetails.ok().put("message", "全局角色删除成功");
    }

    // ==================== 门店角色管理 ====================


    /**
     * 查询指定门店的自定义角色
     */
    @GetMapping("/store/custom/list")
    public ResponseDetails listStoreCustomRoles(@RequestParam Long storeId) {
        storeAuthHelper.checkStoreOwner(storeId);
        return ResponseDetails.ok().put("data", roleDataService.findStoreCustomRoles(storeId));
    }

    /**
     * 创建门店自定义角色
     * 仅本店店主可操作，角色自动绑定到当前门店
     */
    @PostMapping("/store/custom/add")
    public ResponseDetails addStoreCustomRole(@RequestParam Long storeId,
                                              @RequestBody RoleData roleData) {
        storeAuthHelper.checkStoreOwner(storeId);
        roleData.setDomain(RoleDomainEnum.STORE.getCode());
        roleData.setStoreId(storeId);
        return ResponseDetails.ok().put("data", roleDataService.save(roleData));
    }

    /**
     * 更新门店自定义角色
     */
    @PostMapping("/store/custom/update")
    public ResponseDetails updateStoreCustomRole(@RequestParam Long storeId,
                                                 @RequestBody RoleData roleData) {
        storeAuthHelper.checkStoreOwner(storeId);
        RoleData existing = roleDataService.findById(roleData.getId())
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        if (!existing.isStore()) {
            throw new BusinessException(400, "只能修改门店域角色");
        }
        if (existing.getStoreId() != null && !existing.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "只能修改本店的角色");
        }
        roleData.setDomain(RoleDomainEnum.STORE.getCode());
        return ResponseDetails.ok().put("data", roleDataService.update(roleData));
    }

    /**
     * 删除门店自定义角色
     */
    @PostMapping("/store/custom/delete/{id}")
    public ResponseDetails deleteStoreCustomRole(@RequestParam Long storeId,
                                                 @PathVariable Long id) {
        storeAuthHelper.checkStoreOwner(storeId);
        RoleData existing = roleDataService.findById(id)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        if (!existing.isStore()) {
            throw new BusinessException(400, "只能删除门店域角色");
        }
        if (existing.getStoreId() != null && !existing.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "只能删除本店的角色");
        }
        roleDataService.deleteById(id);
        return ResponseDetails.ok().put("message", "门店角色删除成功");
    }

    // ==================== 角色-权限关联管理 ====================

    /**
     * 为角色分配权限（域隔离：全局角色只能分配全局权限，门店角色只能分配门店权限）
     * 权限校验：
     * - 全局角色：需要 role:global:manage 权限
     * - 门店角色：仅本店店主或超级管理员可操作
     */
    @SaCheckLogin
    @PostMapping("/permission/assign")
    public ResponseDetails assignPermission(@RequestParam Long roleId,
                                            @RequestParam Long permissionId) {
        RoleData role = roleDataService.findById(roleId)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        PermissionData permission = permissionDataService.findById(permissionId)
                .orElseThrow(() -> new BusinessException(404, "权限不存在"));

        // 域隔离校验：全局角色只能绑定全局权限，门店角色只能绑定门店权限
        if (role.isGlobal() && !permission.isGlobal()) {
            throw new BusinessException(400, "全局角色只能绑定全局域权限");
        }
        if (role.isStore() && !permission.isStore()) {
            throw new BusinessException(400, "门店角色只能绑定门店域权限");
        }

        // 操作权限校验
        if (role.isGlobal()) {
            StpUtil.checkPermission("role:global:manage");
        } else {
            // 门店模板角色(storeId=null)仅超管可改；门店自定义角色仅本店店主可改
            if (role.getStoreId() != null) {
                storeAuthHelper.checkStoreOwner(role.getStoreId());
            } else {
                StpUtil.checkPermission("role:global:manage");
            }
        }

        RolePermissionData rp = new RolePermissionData();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return ResponseDetails.ok().put("data", rolePermissionDataService.save(rp));
    }

    /**
     * 移除角色权限（权限校验规则与分配相同）
     */
    @SaCheckLogin
    @PostMapping("/permission/remove")
    public ResponseDetails removePermission(@RequestParam Long roleId,
                                            @RequestParam Long permissionId) {
        RoleData role = roleDataService.findById(roleId)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));

        if (role.isGlobal()) {
            StpUtil.checkPermission("role:global:manage");
        } else {
            if (role.getStoreId() != null) {
                storeAuthHelper.checkStoreOwner(role.getStoreId());
            } else {
                StpUtil.checkPermission("role:global:manage");
            }
        }

        rolePermissionDataService.deleteByRoleIdAndPermissionId(roleId, permissionId);
        return ResponseDetails.ok().put("message", "权限移除成功");
    }
}
