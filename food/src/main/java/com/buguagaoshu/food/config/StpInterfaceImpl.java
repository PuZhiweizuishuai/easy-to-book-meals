package com.buguagaoshu.food.config;

import cn.dev33.satoken.stp.StpInterface;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.service.UserRoleDataService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限/角色接口实现（RBAC-D 域感知）
 * <p>
 * Sa-Token 调用此实现类获取当前登录用户的角色列表和权限列表
 * <p>
 * 返回策略：
 * - getRoleList: 返回用户所有角色编码（全局+门店），用于 @SaCheckRole 注解
 * - getPermissionList: 返回用户所有权限编码（全局+门店），用于 @SaCheckPermission 注解
 * <p>
 * 门店维度的精细权限校验由 StoreAuthHelper 完成，
 * 此处返回全量权限列表用于 Sa-Token 注解级别的粗粒度校验
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final UserRoleDataService userRoleDataService;

    public StpInterfaceImpl(UserRoleDataService userRoleDataService) {
        this.userRoleDataService = userRoleDataService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        System.out.println("获取登录用户权限：" + userId + "  " + loginType);
        List<UserRoleData> userRoles = userRoleDataService.findByUserId(userId);

        List<String> permissionCodes = new ArrayList<>();
        for (UserRoleData ur : userRoles) {
            List<String> codes = RolePermissionCache.getPermissionCodesByRoleId(ur.getRoleId());
            permissionCodes.addAll(codes);
        }
        return permissionCodes;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        System.out.println("获取登录用户角色：" + userId + "  " + loginType);
        List<UserRoleData> userRoles = userRoleDataService.findByUserId(userId);

        List<String> roleCodes = new ArrayList<>();
        for (UserRoleData ur : userRoles) {
            RoleData role = RolePermissionCache.ROLE_MAP.get(ur.getRoleId());
            if (role != null && role.getStatus() == 0) {
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
    }
}
