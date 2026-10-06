package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册入参
 * <p>
 * 普通用户使用手机号或邮箱注册：{@link #target} 为手机号或邮箱，
 * {@link #code} 为对应手机号/邮箱收到的验证码，由后端校验通过后方可建号。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class RegisterDto {

    /**
     * 注册凭证：手机号 或 邮箱（二选一，填入同一字段，服务端按格式区分）
     */
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;

    /**
     * 手机号/邮箱验证码
     */
    @NotBlank(message = "请输入验证码")
    private String code;

    /**
     * 登录密码
     */
    @NotBlank(message = "请设置密码")
    private String password;

    /**
     * 昵称（可选，为空时由前端/后端默认生成）
     */
    private String nickname;
}
