package com.buguagaoshu.food.enums;

import lombok.Getter;

@Getter
public enum RbacPermissionCode {

    USER_MANAGE("user:manage", "用户管理", "user", RoleDomainEnum.GLOBAL, "管理用户信息、分配全局角色", 1),
    USER_VIEW("user:view", "查看用户", "user", RoleDomainEnum.GLOBAL, "查看用户基本信息", 2),
    STORE_AUDIT("store:audit", "店面审核", "store", RoleDomainEnum.GLOBAL, "审核门店入驻申请", 3),
    STORE_VIEW_GLOBAL("store:view", "查看店面", "store", RoleDomainEnum.GLOBAL, "查看店面基本信息（平台级）", 4),
    SYSTEM_CONFIG("system:config", "系统配置", "system", RoleDomainEnum.GLOBAL, "管理系统配置参数", 5),
    STATS_PLATFORM("stats:platform", "平台统计", "stats", RoleDomainEnum.GLOBAL, "查看平台经营统计数据", 6),
    REVIEW_AUDIT("review:audit", "评价审核", "review", RoleDomainEnum.GLOBAL, "审核用户评价内容", 7),
    ROLE_GLOBAL_MANAGE("role:global:manage", "全局角色管理", "role", RoleDomainEnum.GLOBAL, "创建、修改、删除全局角色，分配全局角色给用户", 8),
    ROLE_GLOBAL_VIEW("role:global:view", "查看全局角色", "role", RoleDomainEnum.GLOBAL, "查看全局角色列表和详情", 9),
    PERMISSION_VIEW("permission:view", "查看权限", "permission", RoleDomainEnum.GLOBAL, "查看权限列表和详情", 10),

    STORE_MANAGE("store:manage", "店面管理", "store", RoleDomainEnum.STORE, "管理自己店面的基本信息", 1),
    STORE_VIEW("store:view", "查看店面", "store", RoleDomainEnum.STORE, "查看店面详细信息（门店级）", 2),
    DISH_MANAGE("dish:manage", "菜品管理", "dish", RoleDomainEnum.STORE, "新增、修改、删除菜品", 3),
    DISH_VIEW("dish:view", "查看菜品", "dish", RoleDomainEnum.STORE, "查看菜品信息", 4),
    ORDER_MANAGE("order:manage", "订单管理", "order", RoleDomainEnum.STORE, "处理订单、修改订单状态", 5),
    ORDER_VIEW("order:view", "查看订单", "order", RoleDomainEnum.STORE, "查看订单信息", 6),
    TABLE_MANAGE("table:manage", "餐桌管理", "table", RoleDomainEnum.STORE, "管理餐桌信息、状态", 7),
    TABLE_VIEW("table:view", "查看餐桌", "table", RoleDomainEnum.STORE, "查看餐桌信息", 8),
    DELIVERY_MANAGE("delivery:manage", "配送管理", "delivery", RoleDomainEnum.STORE, "管理配送任务", 9),
    DELIVERY_VIEW("delivery:view", "查看配送", "delivery", RoleDomainEnum.STORE, "查看配送信息", 10),
    REVIEW_MANAGE("review:manage", "评价管理", "review", RoleDomainEnum.STORE, "管理用户评价、回复评价", 11),
    REVIEW_VIEW("review:view", "查看评价", "review", RoleDomainEnum.STORE, "查看评价信息", 12),
    STATS_STORE("stats:store", "店面统计", "stats", RoleDomainEnum.STORE, "查看本店经营统计数据", 13),
    INVENTORY_MANAGE("inventory:manage", "库存管理", "inventory", RoleDomainEnum.STORE, "管理库存信息", 14),
    INVENTORY_VIEW("inventory:view", "查看库存", "inventory", RoleDomainEnum.STORE, "查看库存信息", 15);

    private final String code;
    private final String name;
    private final String module;
    private final RoleDomainEnum domain;
    private final String description;
    private final int sort;

    RbacPermissionCode(String code, String name, String module, RoleDomainEnum domain, String description, int sort) {
        this.code = code;
        this.name = name;
        this.module = module;
        this.domain = domain;
        this.description = description;
        this.sort = sort;
    }
}
