package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店主/服务员协助下单 DTO
 * <p>
 * 继承顾客下单 DTO，额外携带顾客信息（昵称/电话），由店员代客下单。
 * 线下餐桌订单应指定 tableId 或 qrCodeKey 以绑定餐桌。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StaffCreateOrderDto extends CreateOrderDto {

    /**
     * 顾客昵称（展示用快照，可为匿名如"到店顾客"）
     */
    @Size(max = 50, message = "顾客昵称不能超过50个字符")
    private String customerName;

    /**
     * 顾客联系电话（可选，外送订单建议填写）
     */
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String customerPhone;
}
