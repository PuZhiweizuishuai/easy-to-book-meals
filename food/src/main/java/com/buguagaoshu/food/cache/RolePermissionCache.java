package com.buguagaoshu.food.cache;

import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 角色与权限缓存
 * 程序启动时从数据库加载角色和权限数据到内存，减少运行时数据库查询
 * 后续对接 Sa-Token 的 StpInterface 时，从此缓存中获取角色和权限列表
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
public class RolePermissionCache {

    /**
     * 角色ID -> 角色实体
     */
    public static final Map<Long, RoleData> ROLE_MAP = new ConcurrentHashMap<>();

    /**
     * 角色编码 -> 角色实体
     * 便于通过 roleCode 快速查找角色
     */
    public static final Map<String, RoleData> ROLE_CODE_MAP = new ConcurrentHashMap<>();

    /**
     * 权限ID -> 权限实体
     */
    public static final Map<Long, PermissionData> PERMISSION_MAP = new ConcurrentHashMap<>();

    /**
     * 权限编码 -> 权限实体
     * 便于通过 permissionCode 快速查找权限
     */
    public static final Map<String, PermissionData> PERMISSION_CODE_MAP = new ConcurrentHashMap<>();

    /**
     * 角色ID -> 该角色拥有的权限编码列表
     * 用于根据角色快速获取权限，StpInterface 实现时配合用户角色查询使用
     */
    public static final Map<Long, List<String>> ROLE_PERMISSIONS_MAP = new ConcurrentHashMap<>();
}
