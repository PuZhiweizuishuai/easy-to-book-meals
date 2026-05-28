package com.buguagaoshu.food.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.RoleDataService;
import com.buguagaoshu.food.service.UserRoleDataService;
import com.buguagaoshu.food.enums.RbacRoleCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门店权限校验工具（RBAC-D 门店域精细校验）
 * <p>
 * Sa-Token 的 @SaCheckPermission 注解只能做粗粒度的权限校验（不区分门店域），
 * 门店维度的精细权限校验需要通过此工具类完成。
 * <p>
 * 使用方式（在 Controller 或 Service 中调用）：
 * <pre>
 *     StoreAuthHelper.checkStorePermission(storeId, "dish:manage");
 * </pre>
 * <p>
 * 校验逻辑：
 * 1. 当前登录用户必须在该门店拥有某个门店域角色
 * 2. 该门店域角色必须包含指定的权限编码
 * 3. 超级管理员(SUPER_ADMIN)默认拥有所有门店权限
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@Component
public class StoreAuthHelper {

    private final UserRoleDataService userRoleDataService;
    private final RoleDataService roleDataService;

    public StoreAuthHelper(UserRoleDataService userRoleDataService,
                           RoleDataService roleDataService) {
        this.userRoleDataService = userRoleDataService;
        this.roleDataService = roleDataService;
    }

    /**
     * 校验当前登录用户是否拥有指定门店的指定权限
     * 校验失败抛出 BusinessException(403)
     */
    public void checkStorePermission(Long storeId, String permissionCode) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (isSuperAdmin(userId)) {
            return;
        }

        if (hasStorePermission(userId, storeId, permissionCode)) {
            return;
        }

        throw new BusinessException(403, "无此门店操作权限: " + permissionCode);
    }

    /**
     * 校验当前登录用户是否拥有指定门店的任意一个权限
     */
    public void checkStoreAnyPermission(Long storeId, String... permissionCodes) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (isSuperAdmin(userId)) {
            return;
        }

        for (String code : permissionCodes) {
            if (hasStorePermission(userId, storeId, code)) {
                return;
            }
        }

        throw new BusinessException(403, "无此门店操作权限");
    }

    /**
     * 校验当前登录用户是否为指定门店的店主
     */
    public void checkStoreOwner(Long storeId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (isSuperAdmin(userId)) {
            return;
        }

        if (isStoreOwner(storeId, userId)) {
            return;
        }

        throw new BusinessException(403, "只有本店店主或超级管理员才能执行此操作");
    }

    private boolean hasStorePermission(Long userId, Long storeId, String permissionCode) {
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreIdAndUserId(storeId, userId);

        for (UserRoleData ur : storeRoles) {
            List<String> permCodes = RolePermissionCache.getPermissionCodesByRoleId(ur.getRoleId());
            if (permCodes.contains(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuperAdmin(Long userId) {
        RoleData superAdminRole = roleDataService.findByRoleCode(RbacRoleCode.SUPER_ADMIN.getCode()).orElse(null);
        if (superAdminRole == null) {
            return false;
        }
        List<UserRoleData> sysRoles = userRoleDataService.findSystemRolesByUserId(userId);
        return sysRoles.stream().anyMatch(ur -> ur.getRoleId().equals(superAdminRole.getId()));
    }

    private boolean isStoreOwner(Long storeId, Long userId) {
        RoleData storeOwnerRole = roleDataService.findByRoleCode(RbacRoleCode.STORE_OWNER.getCode()).orElse(null);
        if (storeOwnerRole == null) {
            return false;
        }
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreIdAndUserId(storeId, userId);
        return storeRoles.stream().anyMatch(ur -> ur.getRoleId().equals(storeOwnerRole.getId()));
    }
}
