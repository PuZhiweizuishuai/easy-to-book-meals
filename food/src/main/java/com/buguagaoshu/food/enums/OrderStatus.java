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
     * 校验状态流转是否合法
     */
    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, List.of()).contains(target);
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
