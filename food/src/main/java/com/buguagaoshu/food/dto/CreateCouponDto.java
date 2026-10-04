package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 创建优惠券模板请求
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Getter
@Setter
public class CreateCouponDto {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotBlank(message = "券名称不能为空")
    @Size(max = 100, message = "券名称不能超过100个字符")
    private String name;

    /**
     * 券类型：1-满减券 2-折扣券 3-无门槛券
     * @see com.buguagaoshu.food.enums.CouponType
     */
    @NotNull(message = "券类型不能为空")
    private Integer type;

    /** 使用门槛（满减券/折扣券必填，须大于 0） */
    private BigDecimal thresholdAmount;

    /** 减免金额（满减券/无门槛券必填，须大于 0） */
    private BigDecimal reduceAmount;

    /** 折扣率（折扣券必填，取值 (0,1)，如 0.85 表示 85 折） */
    @DecimalMin(value = "0.001", message = "折扣率必须大于 0")
    @DecimalMax(value = "0.999", message = "折扣率必须小于 1")
    private BigDecimal discountRate;

    /** 折扣券最高优惠金额（可选） */
    private BigDecimal maxDiscountAmount;

    /** 发行数量（生成券码数量） */
    @NotNull(message = "发行数量不能为空")
    @Min(value = 1, message = "发行数量至少为 1")
    @Max(value = 10000, message = "单次发行数量不能超过 10000")
    private Integer totalCount;

    /** 每人限领数量，0 表示不限 */
    private Integer perLimit;

    /** 生效时间（毫秒时间戳，为空表示立即生效） */
    private Long startTime;

    /** 失效时间（毫秒时间戳，为空或 0 表示长期有效） */
    private Long endTime;

    @Size(max = 255, message = "使用说明不能超过255个字符")
    private String description;
}
