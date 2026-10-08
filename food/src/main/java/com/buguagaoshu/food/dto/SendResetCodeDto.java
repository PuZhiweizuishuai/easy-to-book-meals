package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送「忘记密码」验证码入参
 * <p>
 * 仅需账号已绑定的手机号或邮箱，场景在接口内固定为 RESET_PASSWORD。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class SendResetCodeDto {

    /**
     * 重置凭证：手机号 或 邮箱
     */
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;
}
