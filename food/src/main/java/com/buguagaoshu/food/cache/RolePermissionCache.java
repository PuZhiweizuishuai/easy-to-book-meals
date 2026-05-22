package com.buguagaoshu.food.cache;

import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.enums.RoleDomainEnum;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 角色与权限缓存（RBAC-D 域感知）
 * <p>
 * 程序启动时从数据库加载角色和权限数据到内存，减少运行时数据库查询
 * Sa-Token 的 StpInterface 实现类从此缓存中获取角色和权限列表
 * <p>
 * 缓存结构按域(GLOBAL/STORE)隔离，支持：
 * 1. 按角色ID获取角色实体及权限列表
 * 2. 按域筛选角色和权限
 * 3. 按角色编码快速查找
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
public class RolePermissionCache {

    public static final Map<Long, RoleData> ROLE_MAP = new ConcurrentHashMap<>();

    public static final Map<String, RoleData> ROLE_CODE_MAP = new ConcurrentHashMap<>();

    public static final Map<Long, PermissionData> PERMISSION_MAP = new ConcurrentHashMap<>();

    public static final Map<Long, List<String>> ROLE_PERMISSIONS_MAP = new ConcurrentHashMap<>();

    public static List<RoleData> getRolesByDomain(String domain) {
        return ROLE_MAP.values().stream()
                .filter(r -> domain.equals(r.getDomain()))
                .collect(Collectors.toList());
    }

    public static List<RoleData> getGlobalRoles() {
        return getRolesByDomain(RoleDomainEnum.GLOBAL.getCode());
    }

    public static List<RoleData> getStoreRoles() {
        return getRolesByDomain(RoleDomainEnum.STORE.getCode());
    }

    public static List<PermissionData> getPermissionsByDomain(String domain) {
        return PERMISSION_MAP.values().stream()
                .filter(p -> domain.equals(p.getDomain()))
                .collect(Collectors.toList());
    }

    public static List<PermissionData> getGlobalPermissions() {
        return getPermissionsByDomain(RoleDomainEnum.GLOBAL.getCode());
    }

    public static List<PermissionData> getStorePermissions() {
        return getPermissionsByDomain(RoleDomainEnum.STORE.getCode());
    }

    public static List<String> getPermissionCodesByRoleId(Long roleId) {
        return ROLE_PERMISSIONS_MAP.getOrDefault(roleId, Collections.emptyList());
    }

    public static void clear() {
        ROLE_MAP.clear();
        ROLE_CODE_MAP.clear();
        PERMISSION_MAP.clear();
        ROLE_PERMISSIONS_MAP.clear();
    }
}
