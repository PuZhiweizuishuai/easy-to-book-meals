package com.buguagaoshu.food.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建订单 DTO（顾客扫码点餐/自主下单）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Data
public class CreateOrderDto {

    /**
     * 门店ID
     */
    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    /**
     * 餐桌ID（堂食点餐时二选一：tableId 或 qrCodeKey）
     */
    private Long tableId;

    /**
     * 餐桌二维码标识（扫码点餐时通过二维码定位餐桌）
     */
    private String qrCodeKey;

    /**
     * 订单类型（0-堂食 1-外送 2-线上），不传默认按场景判定：
     * - 扫码点餐（qrCodeKey 不为空）默认堂食
     * - 其它默认线上订单
     * @see com.buguagaoshu.food.enums.OrderType
     */
    private Integer orderType;

    /**
     * 顾客联系电话（外送订单必填，用于配送）
     */
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    /**
     * 收货地址（外送订单必填）
     */
    @Size(max = 255, message = "收货地址不能超过255个字符")
    private String deliveryAddress;

    /**
     * 订单备注
     */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /**
     * 订单明细
     */
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        /**
         * SKU ID
         */
        @NotNull(message = "SKU不能为空")
        private Long skuId;

        /**
         * 购买数量
         */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        @Max(value = 99, message = "单份菜品数量不能超过99")
        private Integer quantity;
    }
}
