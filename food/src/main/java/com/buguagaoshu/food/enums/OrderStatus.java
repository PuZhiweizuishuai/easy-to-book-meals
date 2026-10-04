package com.buguagaoshu.food.enums;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 订单状态机
 * <p>
 * 状态流转：
 * PENDING(待接单) → CONFIRMED(已接单) → COOKING(制作中) → READY(已出餐) → COMPLETED(已完成)
 * PENDING(待接单) → CANCELLED(已取消)   顾客取消 / 店家拒单 / 超时自动取消
 * CONFIRMED(已接单) → CANCELLED(已取消)  店家拒单（需恢复库存）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
public enum OrderStatus {
    PENDING("待接单", 0),
    CONFIRMED("已接单", 1),
    COOKING("制作中", 2),
    READY("已出餐", 3),
    COMPLETED("已完成", 4),
    CANCELLED("已取消", 5);

    private final String name;
    private final int code;

    /**
     * 合法状态流转表：key-当前状态, value-允许流转的目标状态
     */
    private static final Map<OrderStatus, List<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, List.of(CONFIRMED, CANCELLED),
            CONFIRMED, List.of(COOKING, CANCELLED),
            COOKING, List.of(READY),
            READY, List.of(COMPLETED),
            COMPLETED, List.of(),
            CANCELLED, List.of()
    );

    OrderStatus(String name, int code) {
        this.name = name;
        this.code = code;
    }

    /**
     * 校验状态流转是否合法（严格状态机，仅允许相邻状态）
     */
    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, List.of()).contains(target);
    }

    /**
     * 校验是否可以「自动推进」到目标状态（允许跳过中间态）
     * <p>
     * 出餐已下沉到菜品级，店员可能未点击「接单/开始制作」就直接给某个菜品出餐。
     * 此时订单状态仍停留在 PENDING/CONFIRMED，若严格走状态机则永远无法到达 READY，
     * 订单卡在「制作中」导致无法结算。
     * <p>
     * 因此菜品出餐驱动的状态推进采用「顺序递进」规则：
     * 只要目标状态在当前状态之后（PENDING→CONFIRMED→COOKING→READY 单调递增），
     * 且不涉及终态（已完成/已取消必须显式操作），即允许自动推进。
     * <p>
     * 例：PENDING 直接推进到 READY（全部菜品出餐）、CONFIRMED 推进到 COOKING（部分出餐）。
     *
     * @param target 目标状态
     * @return true=允许自动推进
     */
    public boolean canAdvanceTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        // 终态只能由显式操作（结算/取消）进入，且终态不可再流转
        if (this == COMPLETED || this == CANCELLED
                || target == COMPLETED || target == CANCELLED) {
            return false;
        }
        return target.code > this.code;
    }

    /**
     * 按状态码解析枚举，非法码返回 null
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
