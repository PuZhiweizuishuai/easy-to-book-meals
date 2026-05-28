package com.buguagaoshu.food.enums;

import lombok.Getter;

@Getter
public enum RbacRoleCode {

    SUPER_ADMIN("SUPER_ADMIN", "超级管理员", RoleDomainEnum.GLOBAL, "拥有系统所有权限，可管理平台级数据和全局角色分配", 0),
    PLATFORM_OPERATOR("PLATFORM_OPERATOR", "平台运营", RoleDomainEnum.GLOBAL, "负责平台日常运营，管理用户和门店审核", 1),
    PLATFORM_AUDITOR("PLATFORM_AUDITOR", "平台审核", RoleDomainEnum.GLOBAL, "负责门店入驻审核和内容审核", 2),

    STORE_OWNER("STORE_OWNER", "店铺老板", RoleDomainEnum.STORE, "创建和管理自己店面的老板，可管理店内所有事务和人员角色", 10),
    STORE_CASHIER("STORE_CASHIER", "收银员", RoleDomainEnum.STORE, "处理订单收银、查看订单和菜品", 11),
    STORE_KITCHEN("STORE_KITCHEN", "后厨", RoleDomainEnum.STORE, "查看订单和菜品，管理库存", 12),
    STORE_WAITER("STORE_WAITER", "服务员", RoleDomainEnum.STORE, "管理餐桌、查看订单和菜品", 13),
    STORE_INVENTORY("STORE_INVENTORY", "库存管理员", RoleDomainEnum.STORE, "管理菜品和库存", 14),

    CUSTOMER("CUSTOMER", "顾客", RoleDomainEnum.GLOBAL, "普通消费者，可浏览菜品、下单、评价等", 20);

    private final String code;
    private final String name;
    private final RoleDomainEnum domain;
    private final String description;
    private final int sort;

    RbacRoleCode(String code, String name, RoleDomainEnum domain, String description, int sort) {
        this.code = code;
        this.name = name;
        this.domain = domain;
        this.description = description;
        this.sort = sort;
    }
}
