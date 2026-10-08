package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码重置密码入参
 * <p>
 * 用户无需登录，凭账号已绑定的手机号/邮箱及其验证码重置密码。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class ResetPasswordDto {

    /**
     * 重置凭证：手机号 或 邮箱
     */
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;

    /**
     * 手机号/邮箱验证码
     */
    @NotBlank(message = "请输入验证码")
    private String code;

    /**
     * 新密码明文
     */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在 6-32 位之间")
    private String newPassword;
}
