package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.SystemSettingsData;
import com.buguagaoshu.food.dto.UpdateSystemSettingsDto;
import com.buguagaoshu.food.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置服务
 * <p>
 * 单行配置 + 进程内缓存：业务代码（注册、登录限流）直接读取缓存，零数据库开销；
 * 超级后台修改落库成功后刷新缓存，实时生效。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Service
public class SystemSettingsService {

    /** 默认开放注册（保证升级本功能后现有注册流程不被意外关闭） */
    public static final boolean DEFAULT_REGISTRATION_ENABLED = true;

    /** 默认登录失败次数：3 次 */
    public static final int DEFAULT_MAX_LOGIN_FAIL_COUNT = 3;

    /** 默认锁定时长：10 分钟 */
    public static final int DEFAULT_LOGIN_LOCK_MINUTES = 10;

    private final SystemSettingsRepository repository;

    /** 内存缓存（首次访问时初始化） */
    private volatile SystemSettingsData cache;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取系统设置（读缓存，缺失时从库加载，库空则写入默认值）
     */
    public SystemSettingsData getSettings() {
        SystemSettingsData snapshot = cache;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (cache == null) {
                cache = repository.findById(SystemSettingsData.SINGLETON_ID)
                        .orElseGet(this::createDefaultSettings);
            }
            return cache;
        }
    }

    public boolean isRegistrationEnabled() {
        Boolean enabled = getSettings().getRegistrationEnabled();
        return enabled == null ? DEFAULT_REGISTRATION_ENABLED : enabled;
    }

    public int getMaxLoginFailCount() {
        Integer value = getSettings().getMaxLoginFailCount();
        return value == null ? DEFAULT_MAX_LOGIN_FAIL_COUNT : value;
    }

    public int getLoginLockMinutes() {
        Integer value = getSettings().getLoginLockMinutes();
        return value == null ? DEFAULT_LOGIN_LOCK_MINUTES : value;
    }

    /**
     * 更新系统设置（仅更新非 null 字段），落库后刷新缓存
     */
    @Transactional(rollbackFor = Exception.class)
    public SystemSettingsData updateSettings(UpdateSystemSettingsDto dto) {
        SystemSettingsData settings = repository.findById(SystemSettingsData.SINGLETON_ID)
                .orElseGet(this::buildDefaultSettings);

        long now = System.currentTimeMillis();
        if (settings.getCreateTime() == null) {
            settings.setCreateTime(now);
        }

        if (dto.getRegistrationEnabled() != null) {
            settings.setRegistrationEnabled(dto.getRegistrationEnabled());
        }
        if (dto.getMaxLoginFailCount() != null) {
            settings.setMaxLoginFailCount(dto.getMaxLoginFailCount());
        }
        if (dto.getLoginLockMinutes() != null) {
            settings.setLoginLockMinutes(dto.getLoginLockMinutes());
        }
        settings.setUpdateTime(now);

        SystemSettingsData saved = repository.save(settings);
        // 刷新缓存，业务侧立即读到最新配置
        cache = saved;
        return saved;
    }

    /** 构造默认设置并持久化 */
    private SystemSettingsData createDefaultSettings() {
        SystemSettingsData settings = buildDefaultSettings();
        long now = System.currentTimeMillis();
        settings.setCreateTime(now);
        settings.setUpdateTime(now);
        return repository.save(settings);
    }

    private SystemSettingsData buildDefaultSettings() {
        SystemSettingsData settings = new SystemSettingsData();
        settings.setId(SystemSettingsData.SINGLETON_ID);
        settings.setRegistrationEnabled(DEFAULT_REGISTRATION_ENABLED);
        settings.setMaxLoginFailCount(DEFAULT_MAX_LOGIN_FAIL_COUNT);
        settings.setLoginLockMinutes(DEFAULT_LOGIN_LOCK_MINUTES);
        return settings;
    }
}
