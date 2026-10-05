package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 店主邀请员工加入门店 DTO
 * <p>
 * 以手机号定位员工（店主无需知道系统用户ID），支持一次邀请多个角色
 * （如同一人既是服务员又是收银员）。邀请产生待确认通知，员工同意后才真正赋权。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@Data
public class StaffInviteDto {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "请至少选择一个角色")
    private List<Long> roleIds;
}
