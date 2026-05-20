package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.repository.PermissionDataRepository;
import com.buguagaoshu.food.repository.RoleDataRepository;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色与权限数据初始化
 * <p>
 * 系统启动时：
 * 1. 初始化默认角色和权限数据（若数据库中不存在则插入）
 * 2. 建立角色与权限的默认关联关系
 * 3. 将角色和权限数据加载到内存缓存
 * <p>
 * 后续对接 Sa-Token 时，StpInterface 实现类可直接从 RolePermissionCache 获取数据
 * TODO 批量查询
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Slf4j
@Component
@Order(1)
public class RolePermissionDataInitializer implements CommandLineRunner {

    private final RoleDataRepository roleDataRepository;
    private final PermissionDataRepository permissionDataRepository;
    private final RolePermissionDataRepository rolePermissionDataRepository;

    public RolePermissionDataInitializer(RoleDataRepository roleDataRepository,
                                         PermissionDataRepository permissionDataRepository,
                                         RolePermissionDataRepository rolePermissionDataRepository) {
        this.roleDataRepository = roleDataRepository;
        this.permissionDataRepository = permissionDataRepository;
        this.rolePermissionDataRepository = rolePermissionDataRepository;
    }

    @Override
    public void run(String... args) {
        log.info("开始初始化角色与权限数据...");
        initRoles();
        initPermissions();
        initRolePermissionRelations();
        loadCache();
        log.info("角色与权限数据初始化完成");
    }

    // ==================== 角色定义 ====================

    /**
     * 默认角色定义
     * sort 值越小优先级越高
     */
    private static final String[][] DEFAULT_ROLES = {
            // roleCode, roleName, description, sort
            {"SUPER_ADMIN", "超级管理员", "拥有系统所有权限，可管理所有店面和用户", "0"},
            {"STORE_ADMIN", "店面管理员", "创建和管理自己店面的老板，可管理店内所有事务", "1"},
            {"STORE_CLERK", "店员", "店面员工，可处理订单、管理菜品等日常操作", "2"},
            {"DELIVERY_RIDER", "配送员", "负责外卖订单的配送", "3"},
            {"CUSTOMER", "顾客", "普通消费者，可浏览菜品、下单、评价等", "4"},
    };

    // ==================== 权限定义 ====================

    /**
     * 默认权限定义
     * 格式: permissionCode, permissionName, module, description, sort
     */
    private static final String[][] DEFAULT_PERMISSIONS = {
            // 用户模块
            {"user:manage", "用户管理", "user", "管理用户信息、分配角色", "1"},
            {"user:view", "查看用户", "user", "查看用户基本信息", "2"},

            // 店面模块
            {"store:manage", "店面管理", "store", "创建和管理店面信息", "1"},
            {"store:view", "查看店面", "store", "查看店面基本信息", "2"},

            // 菜品模块
            {"dish:manage", "菜品管理", "dish", "新增、修改、删除菜品", "1"},
            {"dish:view", "查看菜品", "dish", "查看菜品信息", "2"},

            // 订单模块
            {"order:manage", "订单管理", "order", "处理订单、修改订单状态", "1"},
            {"order:view", "查看订单", "order", "查看订单信息", "2"},

            // 餐桌模块
            {"table:manage", "餐桌管理", "table", "管理餐桌信息、状态", "1"},
            {"table:view", "查看餐桌", "table", "查看餐桌信息", "2"},

            // 配送模块
            {"delivery:manage", "配送管理", "delivery", "管理配送任务、配送员", "1"},
            {"delivery:view", "查看配送", "delivery", "查看配送信息", "2"},

            // 评价模块
            {"review:manage", "评价管理", "review", "管理用户评价、回复评价", "1"},
            {"review:view", "查看评价", "review", "查看评价信息", "2"},

            // 统计模块
            {"stats:view", "查看统计", "stats", "查看经营统计数据", "1"},

            // 系统模块
            {"system:config", "系统配置", "system", "管理系统配置参数", "1"},
    };

    // ==================== 角色-权限关联定义 ====================

    /**
     * 角色-权限关联定义
     * key: roleCode, value: 该角色拥有的 permissionCode 列表
     */
    private static final java.util.Map<String, List<String>> DEFAULT_ROLE_PERMISSIONS = java.util.Map.of(
            "SUPER_ADMIN", List.of(
                    // 超级管理员拥有所有权限
                    "user:manage", "user:view",
                    "store:manage", "store:view",
                    "dish:manage", "dish:view",
                    "order:manage", "order:view",
                    "table:manage", "table:view",
                    "delivery:manage", "delivery:view",
                    "review:manage", "review:view",
                    "stats:view",
                    "system:config"
            ).stream().toList(),
            "STORE_ADMIN", List.of(
                    "user:view",
                    "store:manage", "store:view",
                    "dish:manage", "dish:view",
                    "order:manage", "order:view",
                    "table:manage", "table:view",
                    "delivery:view",
                    "review:manage", "review:view",
                    "stats:view"
            ),
            "STORE_CLERK", List.of(
                    "dish:view",
                    "order:manage", "order:view",
                    "table:view",
                    "delivery:view",
                    "review:view"
            ),
            "DELIVERY_RIDER", List.of(
                    "order:view",
                    "delivery:view"
            ),
            "CUSTOMER", List.of(
                    "store:view",
                    "dish:view",
                    "order:view",
                    "review:view"
            )
    );

    // ==================== 初始化方法 ====================

    private void initRoles() {
        for (String[] roleDef : DEFAULT_ROLES) {
            if (roleDataRepository.findByRoleCode(roleDef[0]).isEmpty()) {
                RoleData role = new RoleData();
                role.setRoleCode(roleDef[0]);
                role.setRoleName(roleDef[1]);
                role.setDescription(roleDef[2]);
                role.setSort(Integer.parseInt(roleDef[3]));
                role.setStatus(0);
                roleDataRepository.save(role);
                log.info("初始化角色: {} - {}", roleDef[0], roleDef[1]);
            }
        }
    }

    private void initPermissions() {
        for (String[] permDef : DEFAULT_PERMISSIONS) {
            if (permissionDataRepository.findByPermissionCode(permDef[0]).isEmpty()) {
                PermissionData perm = new PermissionData();
                perm.setPermissionCode(permDef[0]);
                perm.setPermissionName(permDef[1]);
                perm.setModule(permDef[2]);
                perm.setDescription(permDef[3]);
                perm.setSort(Integer.parseInt(permDef[4]));
                perm.setStatus(0);
                permissionDataRepository.save(perm);
                log.info("初始化权限: {} - {}", permDef[0], permDef[1]);
            }
        }
    }

    private void initRolePermissionRelations() {
        for (var entry : DEFAULT_ROLE_PERMISSIONS.entrySet()) {
            String roleCode = entry.getKey();
            List<String> permissionCodes = entry.getValue();

            RoleData role = roleDataRepository.findByRoleCode(roleCode).orElse(null);
            if (role == null) {
                log.warn("角色 {} 未找到，跳过权限关联", roleCode);
                continue;
            }

            for (String permCode : permissionCodes) {
                PermissionData perm = permissionDataRepository.findByPermissionCode(permCode).orElse(null);
                if (perm == null) {
                    log.warn("权限 {} 未找到，跳过关联", permCode);
                    continue;
                }

                // 检查关联是否已存在
                boolean exists = rolePermissionDataRepository.findByRoleId(role.getId())
                        .stream().anyMatch(rp -> rp.getPermissionId().equals(perm.getId()));
                if (!exists) {
                    RolePermissionData rp = new RolePermissionData();
                    rp.setRoleId(role.getId());
                    rp.setPermissionId(perm.getId());
                    rolePermissionDataRepository.save(rp);
                }
            }
        }
    }

    /**
     * 将角色、权限及角色-权限关联加载到内存缓存
     * 用户-角色关联不缓存，由 StpInterface 按需查询数据库
     * Sa-Token 自身会对 StpInterface 返回结果做缓存，无需重复缓存
     */
    private void loadCache() {
        // 加载角色缓存
        roleDataRepository.findAll().forEach(role -> {
            RolePermissionCache.ROLE_MAP.put(role.getId(), role);
            RolePermissionCache.ROLE_CODE_MAP.put(role.getRoleCode(), role);
        });

        // 加载权限缓存
        permissionDataRepository.findAll().forEach(perm -> {
            RolePermissionCache.PERMISSION_MAP.put(perm.getId(), perm);
            RolePermissionCache.PERMISSION_CODE_MAP.put(perm.getPermissionCode(), perm);
        });

        // 加载角色-权限关联缓存: roleId -> permissionCode列表
        RolePermissionCache.ROLE_MAP.keySet().forEach(roleId -> {
            List<String> permCodes = new ArrayList<>();
            rolePermissionDataRepository.findByRoleId(roleId).forEach(rp -> {
                PermissionData perm = RolePermissionCache.PERMISSION_MAP.get(rp.getPermissionId());
                if (perm != null && perm.getStatus() == 0) {
                    permCodes.add(perm.getPermissionCode());
                }
            });
            RolePermissionCache.ROLE_PERMISSIONS_MAP.put(roleId, permCodes);
        });

        log.info("角色权限缓存加载完成 - 角色: {}, 权限: {}",
                RolePermissionCache.ROLE_MAP.size(),
                RolePermissionCache.PERMISSION_MAP.size());
    }
}
