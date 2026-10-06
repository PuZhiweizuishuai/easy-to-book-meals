package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改手机号入参
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class ChangePhoneDto {

    /**
     * 原手机号验证码
     * <p>
     * 当前账号已绑定手机号时必填；当前账号未绑定手机号（如邮箱注册）时可不填，
     * 此时仅校验新手机号验证码，属于「首次绑定」而非「更换」。
     */
    private String oldCode;

    /**
     * 新手机号
     */
    @NotBlank(message = "请输入新手机号")
    @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入正确的 11 位手机号")
    private String newPhone;

    /**
     * 新手机号验证码
     */
    @NotBlank(message = "请输入新手机号验证码")
    private String newCode;
}
