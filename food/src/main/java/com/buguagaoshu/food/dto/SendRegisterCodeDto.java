package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送注册验证码入参
 * <p>
 * 仅需注册目标（手机号或邮箱），场景在接口内固定为 REGISTER，
 * 不要求密码与验证码，避免与注册提交入参 {@link RegisterDto} 的必填校验冲突。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class SendRegisterCodeDto {

    /**
     * 注册目标：手机号 或 邮箱
     */
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;
}
