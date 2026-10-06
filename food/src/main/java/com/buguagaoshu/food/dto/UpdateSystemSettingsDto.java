package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新系统设置入参
 * <p>
 * 所有字段可选，仅更新提交的字段（null 表示不修改）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class UpdateSystemSettingsDto {

    /** 是否开放注册 */
    private Boolean registrationEnabled;

    /** 登录密码连续错误次数上限 */
    @Min(value = 1, message = "错误次数最小为 1")
    @Max(value = 100, message = "错误次数最大为 100")
    private Integer maxLoginFailCount;

    /** 登录锁定时长（分钟） */
    @Min(value = 1, message = "锁定时间最小为 1 分钟")
    @Max(value = 1440, message = "锁定时间最大为 1440 分钟（24 小时）")
    private Integer loginLockMinutes;
}
