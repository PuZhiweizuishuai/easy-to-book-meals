package com.buguagaoshu.food.enums;

import lombok.Getter;

/**
 * 订单类型
 * <p>
 * 用于区分不同来源的订单，便于店家分单处理与统计：
 * DINE_IN  — 线下餐桌订单（堂食，扫码点餐，绑定餐桌）
 * TAKEOUT  — 外送订单（外卖配送，需收货地址/电话）
 * ONLINE   — 线上订单（到店自取/打包带走，无需绑定餐桌）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
public enum OrderType {
    DINE_IN("线下餐桌订单", 0),
    TAKEOUT("外送订单", 1),
    ONLINE("线上订单", 2);

    private final String name;
    private final int code;

    OrderType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    /**
     * 按状态码解析枚举，非法或 null 返回 DINE_IN（向后兼容历史数据）
     */
    public static OrderType fromCode(Integer code) {
        if (code == null) {
            return DINE_IN;
        }
        for (OrderType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return DINE_IN;
    }

    /**
     * 判断是否为堂食订单（需要绑定餐桌）
     */
    public boolean isDineIn() {
        return this == DINE_IN;
    }
}
