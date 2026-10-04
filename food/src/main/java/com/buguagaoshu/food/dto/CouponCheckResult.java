package com.buguagaoshu.food.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 优惠券校验/试算结果
 * <p>
 * 结算前调用校验接口得到该券在当前订单金额下的优惠金额，前端据此展示并计算应付。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Getter
@Setter
public class CouponCheckResult {

    /** 券码ID */
    private Long userCouponId;

    /** 券模板ID */
    private Long couponId;

    /** 券码 */
    private String code;

    /** 券名称 */
    private String name;

    /** 券类型码 */
    private Integer type;

    /** 券类型文案 */
    private String typeName;

    /** 使用门槛 */
    private BigDecimal thresholdAmount;

    /** 本单可优惠金额 */
    private BigDecimal discountAmount;

    /** 优惠后应付金额 */
    private BigDecimal payableAmount;

    /** 券说明 */
    private String description;

    /** 券是否已关联用户（已领取） */
    private boolean claimed;

    /** 关联手机号（未关联时为空） */
    private String phone;
}
