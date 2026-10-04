package com.buguagaoshu.food.enums;

import lombok.Getter;

/**
 * 优惠券类型
 * <p>
 * 三种业界主流券模式：
 * 1. 满减券 FULL_REDUCE：满 thresholdAmount 减 reduceAmount
 * 2. 折扣券 DISCOUNT：满 thresholdAmount 打 discountRate 折（如 0.85 = 85 折），
 *    可由 maxDiscountAmount 限制最高优惠金额
 * 3. 无门槛券 NO_THRESHOLD：直接减 reduceAmount，无消费门槛
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Getter
public enum CouponType {
    FULL_REDUCE("满减券", 1),
    DISCOUNT("折扣券", 2),
    NO_THRESHOLD("无门槛券", 3);

    private final String name;
    private final int code;

    CouponType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static CouponType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
