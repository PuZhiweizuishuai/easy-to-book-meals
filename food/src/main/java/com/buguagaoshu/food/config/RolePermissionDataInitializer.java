package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.service.PermissionDataService;
import com.buguagaoshu.food.service.RoleDataService;
import com.buguagaoshu.food.service.RolePermissionDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色与权限数据初始化（RBAC-D 双域隔离）
 * <p>
 * 系统启动时：
 * 1. 初始化全局域角色和权限（平台管理相关）
 * 2. 初始化门店域角色和权限（门店运营相关）
 * 3. 建立角色与权限的默认关联关系（域内绑定，禁止跨域）
 * 4. 将角色和权限数据加载到内存缓存
 * <p>
 * 权限架构：
 * ┌─────────────────────────────────────────────────────┐
 * │                    全局域 (GLOBAL)                    │
 * │  超级管理员 → 平台运营 → 平台审核                      │
 * │  管理用户、审核门店、系统配置、平台统计                   │
 * └─────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────┐
 * │                    门店域 (STORE)                     │
 * │  店铺老板 → 收银员 / 后厨 / 服务员 / 库存管理员         │
 * │  管理菜品、订单、餐桌、配送、评价、库存、门店统计          │
 * └─────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────┐
 * │                  普通用户 (CUSTOMER)                   │
 * │  浏览店面、菜品，下单、评价                              │
 * └─────────────────────────────────────────────────────┘
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Slf4j
@Component
@Order(1)
public class RolePermissionDataInitializer implements CommandLineRunner {

    private final RoleDataService roleDataService;
    private final PermissionDataService permissionDataService;
    private final RolePermissionDataService rolePermissionDataService;

    public RolePermissionDataInitializer(RoleDataService roleDataService,
                                         PermissionDataService permissionDataService,
                                         RolePermissionDataService rolePermissionDataService) {
        this.roleDataService = roleDataService;
        this.permissionDataService = permissionDataService;
        this.rolePermissionDataService = rolePermissionDataService;
    }

    @Override
    public void run(String... args) {
        log.info("开始初始化角色与权限数据（RBAC-D 双域隔离）...");
        initRoles();
        initPermissions();
        initRolePermissionRelations();
        loadCache();
        log.info("角色与权限数据初始化完成 - 全局角色: {}, 门店角色: {}, 全局权限: {}, 门店权限: {}",
                RolePermissionCache.getGlobalRoles().size(),
                RolePermissionCache.getStoreRoles().size(),
                RolePermissionCache.getGlobalPermissions().size(),
                RolePermissionCache.getStorePermissions().size());
    }

    // ==================== 角色定义 ====================

    /**
     * 默认角色定义
     * 格式: roleCode, roleName, domain, description, sort
     * domain: GLOBAL=全局域, STORE=门店域
     */
    private static final String[][] DEFAULT_ROLES = {
            // 全局域角色
            {"SUPER_ADMIN", "超级管理员", "GLOBAL", "拥有系统所有权限，可管理平台级数据和全局角色分配", "0"},
            {"PLATFORM_OPERATOR", "平台运营", "GLOBAL", "负责平台日常运营，管理用户和门店审核", "1"},
            {"PLATFORM_AUDITOR", "平台审核", "GLOBAL", "负责门店入驻审核和内容审核", "2"},

            // 门店域角色
            {"STORE_OWNER", "店铺老板", "STORE", "创建和管理自己店面的老板，可管理店内所有事务和人员角色", "10"},
            {"STORE_CASHIER", "收银员", "STORE", "处理订单收银、查看订单和菜品", "11"},
            {"STORE_KITCHEN", "后厨", "STORE", "查看订单和菜品，管理库存", "12"},
            {"STORE_WAITER", "服务员", "STORE", "管理餐桌、查看订单和菜品", "13"},
            {"STORE_INVENTORY", "库存管理员", "STORE", "管理菜品和库存", "14"},

            // 普通用户
            {"CUSTOMER", "顾客", "GLOBAL", "普通消费者，可浏览菜品、下单、评价等", "20"},
    };

    // ==================== 权限定义 ====================

    /**
     * 默认权限定义
     * 格式: permissionCode, permissionName, module, domain, description, sort
     * domain: GLOBAL=全局域权限, STORE=门店域权限
     * 两套权限池互不通用，不能跨域绑定
     */
    private static final String[][] DEFAULT_PERMISSIONS = {
            // ========== 全局域权限（平台管理） ==========
            {"user:manage", "用户管理", "user", "GLOBAL", "管理用户信息、分配全局角色", "1"},
            {"user:view", "查看用户", "user", "GLOBAL", "查看用户基本信息", "2"},
            {"store:audit", "店面审核", "store", "GLOBAL", "审核门店入驻申请", "3"},
            {"store:view", "查看店面", "store", "GLOBAL", "查看店面基本信息（平台级）", "4"},
            {"system:config", "系统配置", "system", "GLOBAL", "管理系统配置参数", "5"},
            {"stats:platform", "平台统计", "stats", "GLOBAL", "查看平台经营统计数据", "6"},
            {"review:audit", "评价审核", "review", "GLOBAL", "审核用户评价内容", "7"},
            {"role:global:manage", "全局角色管理", "role", "GLOBAL", "创建、修改、删除全局角色，分配全局角色给用户", "8"},
            {"role:global:view", "查看全局角色", "role", "GLOBAL", "查看全局角色列表和详情", "9"},
            {"permission:view", "查看权限", "permission", "GLOBAL", "查看权限列表和详情", "10"},

            // ========== 门店域权限（门店运营） ==========
            {"store:manage", "店面管理", "store", "STORE", "管理自己店面的基本信息", "1"},
            {"store:view", "查看店面", "store", "STORE", "查看店面详细信息（门店级）", "2"},
            {"dish:manage", "菜品管理", "dish", "STORE", "新增、修改、删除菜品", "3"},
            {"dish:view", "查看菜品", "dish", "STORE", "查看菜品信息", "4"},
            {"order:manage", "订单管理", "order", "STORE", "处理订单、修改订单状态", "5"},
            {"order:view", "查看订单", "order", "STORE", "查看订单信息", "6"},
            {"table:manage", "餐桌管理", "table", "STORE", "管理餐桌信息、状态", "7"},
            {"table:view", "查看餐桌", "table", "STORE", "查看餐桌信息", "8"},
            {"delivery:manage", "配送管理", "delivery", "STORE", "管理配送任务", "9"},
            {"delivery:view", "查看配送", "delivery", "STORE", "查看配送信息", "10"},
            {"review:manage", "评价管理", "review", "STORE", "管理用户评价、回复评价", "11"},
            {"review:view", "查看评价", "review", "STORE", "查看评价信息", "12"},
            {"stats:store", "店面统计", "stats", "STORE", "查看本店经营统计数据", "13"},
            {"inventory:manage", "库存管理", "inventory", "STORE", "管理库存信息", "14"},
            {"inventory:view", "查看库存", "inventory", "STORE", "查看库存信息", "15"},
    };

    // ==================== 角色-权限关联定义 ====================

    /**
     * 角色-权限关联定义
     * key: roleCode, value: 该角色拥有的 permissionCode 列表
     * 严格遵循域隔离：全局角色只能绑定全局权限，门店角色只能绑定门店权限
     */
    private static final Map<String, List<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
            "SUPER_ADMIN", List.of(
                    "user:manage", "user:view",
                    "store:audit", "store:view",
                    "system:config",
                    "stats:platform",
                    "review:audit",
                    "role:global:manage", "role:global:view",
                    "permission:view"
            ),
            "PLATFORM_OPERATOR", List.of(
                    "user:manage", "user:view",
                    "store:audit", "store:view",
                    "stats:platform",
                    "review:audit",
                    "role:global:view",
                    "permission:view"
            ),
            "PLATFORM_AUDITOR", List.of(
                    "store:audit", "store:view",
                    "review:audit",
                    "user:view"
            ),
            "STORE_OWNER", List.of(
                    "store:manage", "store:view",
                    "dish:manage", "dish:view",
                    "order:manage", "order:view",
                    "table:manage", "table:view",
                    "delivery:manage", "delivery:view",
                    "review:manage", "review:view",
                    "stats:store",
                    "inventory:manage", "inventory:view"
            ),
            "STORE_CASHIER", List.of(
                    "order:manage", "order:view",
                    "dish:view",
                    "table:view"
            ),
            "STORE_KITCHEN", List.of(
                    "order:view",
                    "dish:view",
                    "inventory:manage", "inventory:view"
            ),
            "STORE_WAITER", List.of(
                    "order:view",
                    "dish:view",
                    "table:manage", "table:view"
            ),
            "STORE_INVENTORY", List.of(
                    "dish:manage", "dish:view",
                    "inventory:manage", "inventory:view"
            ),
            "CUSTOMER", List.of()
    );

    // ==================== 初始化方法 ====================

    private void initRoles() {
        Set<String> existingRoleCodes = roleDataService.findAll()
                .stream()
                .map(RoleData::getRoleCode)
                .collect(Collectors.toSet());

        List<RoleData> roleDataList = new ArrayList<>();
        for (String[] roleDef : DEFAULT_ROLES) {
            if (!existingRoleCodes.contains(roleDef[0])) {
                RoleData role = new RoleData();
                role.setRoleCode(roleDef[0]);
                role.setRoleName(roleDef[1]);
                role.setDomain(roleDef[2]);
                role.setDescription(roleDef[3]);
                role.setSort(Integer.parseInt(roleDef[4]));
                role.setStatus(0);
                role.setStoreId(null);
                roleDataList.add(role);
                log.info("初始化角色: [{}] {} - {}", roleDef[2], roleDef[0], roleDef[1]);
            }
        }
        if (!roleDataList.isEmpty()) {
            roleDataService.saveAll(roleDataList);
        }
    }

    private void initPermissions() {
        Set<String> existingPermKeys = permissionDataService.findAll()
                .stream()
                .map(p -> p.getPermissionCode() + "_" + p.getDomain())
                .collect(Collectors.toSet());

        List<PermissionData> permissionDataList = new ArrayList<>();
        for (String[] permDef : DEFAULT_PERMISSIONS) {
            String permKey = permDef[0] + "_" + permDef[3];
            if (!existingPermKeys.contains(permKey)) {
                PermissionData perm = new PermissionData();
                perm.setPermissionCode(permDef[0]);
                perm.setPermissionName(permDef[1]);
                perm.setModule(permDef[2]);
                perm.setDomain(permDef[3]);
                perm.setDescription(permDef[4]);
                perm.setSort(Integer.parseInt(permDef[5]));
                perm.setStatus(0);
                permissionDataList.add(perm);
                log.info("初始化权限: [{}] {} - {}", permDef[3], permDef[0], permDef[1]);
            }
        }
        if (!permissionDataList.isEmpty()) {
            permissionDataService.saveAll(permissionDataList);
        }
    }

    private void initRolePermissionRelations() {
        List<RoleData> allRoles = roleDataService.findAll();
        Map<String, Long> roleCodeToIdMap = allRoles.stream()
                .collect(Collectors.toMap(RoleData::getRoleCode, RoleData::getId));

        List<PermissionData> allPermissions = permissionDataService.findAll();
        Map<String, Long> permCodeToIdMap = allPermissions.stream()
                .collect(Collectors.toMap(
                        p -> p.getPermissionCode() + "_" + p.getDomain(),
                        PermissionData::getId
                ));

        Set<String> existingRelations = rolePermissionDataService.findAll()
                .stream()
                .map(rp -> rp.getRoleId() + "_" + rp.getPermissionId())
                .collect(Collectors.toSet());

        List<RolePermissionData> rolePermissionDataList = new ArrayList<>();
        for (var entry : DEFAULT_ROLE_PERMISSIONS.entrySet()) {
            String roleCode = entry.getKey();
            List<String> permissionCodes = entry.getValue();

            Long roleId = roleCodeToIdMap.get(roleCode);
            if (roleId == null) {
                log.warn("角色 {} 未找到，跳过权限关联", roleCode);
                continue;
            }

            RoleData role = allRoles.stream()
                    .filter(r -> r.getRoleCode().equals(roleCode))
                    .findFirst().orElse(null);
            if (role == null) {
                continue;
            }

            for (String permCode : permissionCodes) {
                String permKey = permCode + "_" + role.getDomain();
                Long permId = permCodeToIdMap.get(permKey);
                if (permId == null) {
                    log.warn("权限 {} 在 {} 域未找到，跳过关联", permCode, role.getDomain());
                    continue;
                }

                String relationKey = roleId + "_" + permId;
                if (!existingRelations.contains(relationKey)) {
                    RolePermissionData rp = new RolePermissionData();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permId);
                    rolePermissionDataList.add(rp);
                }
            }
        }

        if (!rolePermissionDataList.isEmpty()) {
            rolePermissionDataService.saveAll(rolePermissionDataList);
        }
    }

    private void loadCache() {
        List<RoleData> allRoles = roleDataService.findAll();
        allRoles.forEach(role -> {
            RolePermissionCache.ROLE_MAP.put(role.getId(), role);
            RolePermissionCache.ROLE_CODE_MAP.put(role.getRoleCode(), role);
        });

        List<PermissionData> allPermissions = permissionDataService.findAll();
        allPermissions.forEach(perm -> {
            RolePermissionCache.PERMISSION_MAP.put(perm.getId(), perm);
        });

        Map<Long, List<RolePermissionData>> rolePermissionMap = rolePermissionDataService.findAll()
                .stream()
                .collect(Collectors.groupingBy(RolePermissionData::getRoleId));

        RolePermissionCache.ROLE_MAP.keySet().forEach(roleId -> {
            List<String> permCodes = new ArrayList<>();
            List<RolePermissionData> relations = rolePermissionMap.getOrDefault(roleId, Collections.emptyList());
            for (RolePermissionData rp : relations) {
                PermissionData perm = RolePermissionCache.PERMISSION_MAP.get(rp.getPermissionId());
                if (perm != null && perm.getStatus() == 0) {
                    permCodes.add(perm.getPermissionCode());
                }
            }
            RolePermissionCache.ROLE_PERMISSIONS_MAP.put(roleId, permCodes);
        });

        log.info("角色权限缓存加载完成 - 全局角色: {}, 门店角色: {}, 全局权限: {}, 门店权限: {}",
                RolePermissionCache.getGlobalRoles().size(),
                RolePermissionCache.getStoreRoles().size(),
                RolePermissionCache.getGlobalPermissions().size(),
                RolePermissionCache.getStorePermissions().size());
    }
}
