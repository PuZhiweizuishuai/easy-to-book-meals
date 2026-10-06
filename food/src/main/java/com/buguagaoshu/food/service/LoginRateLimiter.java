package com.buguagaoshu.food.service;

import com.buguagaoshu.food.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 登录失败限流器（防账号密码暴力破解）
 * <p>
 * 账号、IP 双维度独立计数：
 * <ul>
 *   <li>账号维度：同一账号在锁定时长（时间窗口）内密码错误达到上限 → 该账号锁定，
 *       即使更换 IP / 密码正确也无法登录（防止针对某一账号的分布式爆破）；</li>
 *   <li>IP 维度：同一 IP 在窗口内失败达到上限 → 该 IP 锁定，
 *       即使轮换账号也无法登录（防止单 IP 对大量账号撞库）；</li>
 *   <li>用户不存在的登录尝试只计入 IP 维度，避免用不存在的用户名撑大账号表；</li>
 *   <li>登录成功后清除该账号与该 IP 的计数。</li>
 * </ul>
 * 阈值 / 锁定时长来自 {@link SystemSettingsService}（超级后台可配），此处不写死。
 * <p>
 * 单机内存实现，适合当前单体部署；多实例部署时替换为 Redis 实现（接口语义保持不变）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Service
public class LoginRateLimiter {

    /** 账号维度失败计数 */
    private final ConcurrentHashMap<String, Counter> accountCounters = new ConcurrentHashMap<>();

    /** IP 维度失败计数 */
    private final ConcurrentHashMap<String, Counter> ipCounters = new ConcurrentHashMap<>();

    /** 写操作计数，周期性触发过期条目清理 */
    private final AtomicLong writeOps = new AtomicLong(0);

    private static final long CLEANUP_EVERY_WRITES = 200;

    private final SystemSettingsService settingsService;

    public LoginRateLimiter(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * 登录前校验：账号或 IP 处于锁定状态则直接拒绝（429），不进入密码校验
     */
    public void assertLoginAllowed(String username, String clientIp) {
        long now = System.currentTimeMillis();

        Counter account = accountCounters.get(normalize(username));
        if (account != null && account.lockUntil > now) {
            throw new BusinessException(429,
                    "账号密码错误次数过多，该账号已被临时锁定，请" + formatRemain(account.lockUntil - now) + "后再试");
        }

        Counter ip = ipCounters.get(normalizeIp(clientIp));
        if (ip != null && ip.lockUntil > now) {
            throw new BusinessException(429,
                    "当前网络登录失败次数过多，请" + formatRemain(ip.lockUntil - now) + "后再试");
        }
    }

    /**
     * 记录一次登录失败
     *
     * @param username      登录账号
     * @param clientIp      客户端 IP
     * @param accountExists 账号是否真实存在（不存在时仅计 IP，避免伪造用户名撑大计数表）
     */
    public void recordLoginFailure(String username, String clientIp, boolean accountExists) {
        if (accountExists) {
            accountCounters.compute(normalize(username), (k, old) -> advance(old));
        }
        ipCounters.compute(normalizeIp(clientIp), (k, old) -> advance(old));

        if (writeOps.incrementAndGet() % CLEANUP_EVERY_WRITES == 0) {
            purgeExpired();
        }
    }

    /**
     * 登录成功：清除该账号与该 IP 的失败计数
     */
    public void recordLoginSuccess(String username, String clientIp) {
        accountCounters.remove(normalize(username));
        ipCounters.remove(normalizeIp(clientIp));
    }

    /**
     * 推进计数：处理窗口过期 / 锁定到期，再累加一次失败，达到阈值则设置锁定截止时间
     */
    private Counter advance(Counter old) {
        long now = System.currentTimeMillis();
        long windowMs = settingsService.getLoginLockMinutes() * 60_000L;
        int maxFail = settingsService.getMaxLoginFailCount();

        Counter c = old;
        if (c == null) {
            c = new Counter();
            c.firstFailAt = now;
        } else if ((c.lockUntil > 0 && now >= c.lockUntil)
                || (c.lockUntil == 0 && now - c.firstFailAt > windowMs)) {
            // 锁定已到期，或未锁定但计数窗口已过：重新计数
            c.failCount = 0;
            c.firstFailAt = now;
            c.lockUntil = 0;
        }

        c.failCount += 1;
        if (c.failCount >= maxFail) {
            // 从触发本次失败起锁定完整的配置时长
            c.lockUntil = now + windowMs;
        }
        return c;
    }

    /** 清理两个 Map 中锁定已到期、窗口已失效的条目，防止内存膨胀 */
    private void purgeExpired() {
        long now = System.currentTimeMillis();
        accountCounters.entrySet().removeIf(e -> {
            Counter c = e.getValue();
            return c.lockUntil > 0 ? now >= c.lockUntil : now - c.firstFailAt > settingsService.getLoginLockMinutes() * 60_000L;
        });
        ipCounters.entrySet().removeIf(e -> {
            Counter c = e.getValue();
            return c.lockUntil > 0 ? now >= c.lockUntil : now - c.firstFailAt > settingsService.getLoginLockMinutes() * 60_000L;
        });
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeIp(String ip) {
        return ip == null || ip.isBlank() ? "unknown" : ip.trim();
    }

    /** 剩余时间展示：大于 1 分钟显示分钟，否则显示秒 */
    private String formatRemain(long ms) {
        long seconds = (ms + 999) / 1000;
        if (seconds >= 60) {
            long minutes = (seconds + 59) / 60;
            return "约 " + minutes + " 分钟";
        }
        return seconds + " 秒";
    }

    /** 失败计数条目 */
    private static class Counter {
        private int failCount;
        private long firstFailAt;
        /** 锁定截止时间戳，0 表示未锁定 */
        private long lockUntil;
    }
}
