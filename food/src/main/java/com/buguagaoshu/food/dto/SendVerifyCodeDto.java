package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码入参
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class SendVerifyCodeDto {

    /**
     * 手机号
     * <p>
     * 当场景为「校验原手机号」时，服务端忽略本字段，直接取当前账号已绑定的手机号，
     * 防止向任意号码发送原手机验证码。
     */
    private String phone;

    /**
     * 使用场景编码（见 VerificationCodeScene）
     */
    @NotBlank(message = "缺少验证码场景")
    private String scene;
}
