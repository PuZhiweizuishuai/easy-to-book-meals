package com.buguagaoshu.food.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 订单加菜 DTO（未结算前追加菜品）
 * <p>
 * 仅允许在订单处于「待接单 / 已接单」状态时加菜；
 * 制作中及之后状态不允许，避免后厨流程混乱。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Data
public class AddOrderItemsDto {

    @NotEmpty(message = "加菜明细不能为空")
    @Valid
    private List<CreateOrderDto.Item> items;
}
