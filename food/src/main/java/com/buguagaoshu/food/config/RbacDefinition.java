package com.buguagaoshu.food.config;

import com.buguagaoshu.food.enums.RbacPermissionCode;
import com.buguagaoshu.food.enums.RbacRoleCode;

import java.util.*;

public class RbacDefinition {

    private RbacDefinition() {
    }

    public static List<RbacRoleCode> getDefaultRoles() {
        return Arrays.asList(RbacRoleCode.values());
    }

    public static List<RbacPermissionCode> getDefaultPermissions() {
        return Arrays.asList(RbacPermissionCode.values());
    }

    public static Map<RbacRoleCode, List<RbacPermissionCode>> getDefaultRolePermissions() {
        Map<RbacRoleCode, List<RbacPermissionCode>> map = new LinkedHashMap<>();
        map.put(RbacRoleCode.SUPER_ADMIN, List.of(
                RbacPermissionCode.USER_MANAGE, RbacPermissionCode.USER_VIEW,
                RbacPermissionCode.STORE_AUDIT, RbacPermissionCode.STORE_VIEW_GLOBAL,
                RbacPermissionCode.SYSTEM_CONFIG,
                RbacPermissionCode.STATS_PLATFORM,
                RbacPermissionCode.REVIEW_AUDIT,
                RbacPermissionCode.ROLE_GLOBAL_MANAGE, RbacPermissionCode.ROLE_GLOBAL_VIEW,
                RbacPermissionCode.PERMISSION_VIEW
        ));
        map.put(RbacRoleCode.PLATFORM_OPERATOR, List.of(
                RbacPermissionCode.USER_MANAGE, RbacPermissionCode.USER_VIEW,
                RbacPermissionCode.STORE_AUDIT, RbacPermissionCode.STORE_VIEW_GLOBAL,
                RbacPermissionCode.STATS_PLATFORM,
                RbacPermissionCode.REVIEW_AUDIT,
                RbacPermissionCode.ROLE_GLOBAL_VIEW,
                RbacPermissionCode.PERMISSION_VIEW
        ));
        map.put(RbacRoleCode.PLATFORM_AUDITOR, List.of(
                RbacPermissionCode.STORE_AUDIT, RbacPermissionCode.STORE_VIEW_GLOBAL,
                RbacPermissionCode.REVIEW_AUDIT,
                RbacPermissionCode.USER_VIEW
        ));
        map.put(RbacRoleCode.STORE_OWNER, List.of(
                RbacPermissionCode.STORE_MANAGE, RbacPermissionCode.STORE_VIEW,
                RbacPermissionCode.DISH_MANAGE, RbacPermissionCode.DISH_VIEW,
                RbacPermissionCode.ORDER_MANAGE, RbacPermissionCode.ORDER_VIEW,
                RbacPermissionCode.TABLE_MANAGE, RbacPermissionCode.TABLE_VIEW,
                RbacPermissionCode.DELIVERY_MANAGE, RbacPermissionCode.DELIVERY_VIEW,
                RbacPermissionCode.REVIEW_MANAGE, RbacPermissionCode.REVIEW_VIEW,
                RbacPermissionCode.STATS_STORE,
                RbacPermissionCode.INVENTORY_MANAGE, RbacPermissionCode.INVENTORY_VIEW,
                RbacPermissionCode.ROLE_STORE_MANAGE,
                RbacPermissionCode.MEMBER_MANAGE,
                RbacPermissionCode.COUPON_MANAGE, RbacPermissionCode.COUPON_VIEW
        ));
        map.put(RbacRoleCode.STORE_CASHIER, List.of(
                RbacPermissionCode.ORDER_MANAGE, RbacPermissionCode.ORDER_VIEW,
                RbacPermissionCode.DISH_VIEW,
                RbacPermissionCode.TABLE_VIEW,
                RbacPermissionCode.MEMBER_MANAGE,
                RbacPermissionCode.COUPON_VIEW
        ));
        map.put(RbacRoleCode.STORE_KITCHEN, List.of(
                RbacPermissionCode.ORDER_VIEW,
                RbacPermissionCode.DISH_VIEW,
                RbacPermissionCode.INVENTORY_MANAGE, RbacPermissionCode.INVENTORY_VIEW
        ));
        map.put(RbacRoleCode.STORE_WAITER, List.of(
                RbacPermissionCode.ORDER_VIEW,
                RbacPermissionCode.DISH_VIEW,
                RbacPermissionCode.TABLE_MANAGE, RbacPermissionCode.TABLE_VIEW
        ));
        map.put(RbacRoleCode.STORE_INVENTORY, List.of(
                RbacPermissionCode.DISH_MANAGE, RbacPermissionCode.DISH_VIEW,
                RbacPermissionCode.INVENTORY_MANAGE, RbacPermissionCode.INVENTORY_VIEW
        ));
        map.put(RbacRoleCode.CUSTOMER, List.of());
        return map;
    }
}
