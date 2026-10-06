package com.buguagaoshu.food.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 系统设置（单行配置表）
 * <p>
 * 全局只有一条记录，主键固定为 1。由超级后台「系统设置」维护，
 * 后端内存缓存，修改后实时生效，无需重启。
 * <p>
 * 当前包含：
 * <ul>
 *   <li>{@code registrationEnabled} — 是否开放用户注册</li>
 *   <li>{@code maxLoginFailCount}   — 登录密码连续错误上限</li>
 *   <li>{@code loginLockMinutes}    — 触发上限后账号 / IP 的锁定时长（分钟）</li>
 * </ul>
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSettingsData {

    /** 单行配置固定主键 */
    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** 是否开放注册：true=允许注册，false=关闭注册 */
    private Boolean registrationEnabled;

    /** 登录密码连续错误次数上限，达到后账号与 IP 锁定 */
    private Integer maxLoginFailCount;

    /** 登录锁定时长（分钟），同时作为失败计数的时间窗口 */
    private Integer loginLockMinutes;

    private Long createTime;

    private Long updateTime;
}
