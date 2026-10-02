package com.buguagaoshu.food.enums;

import lombok.Getter;

/**
 * 支付状态
 * 支付系统暂未对接，当前支付状态仅作流程标记（堂食扫码点餐为到店支付）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
public enum PayStatus {
    UNPAID("未支付", 0),
    PAID("已支付", 1),
    REFUNDED("已退款", 2);

    private final String name;
    private final int code;

    PayStatus(String name, int code) {
        this.name = name;
        this.code = code;
    }
}
