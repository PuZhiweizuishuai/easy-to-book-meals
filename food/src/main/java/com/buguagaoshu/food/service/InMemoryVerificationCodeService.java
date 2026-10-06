package com.buguagaoshu.food.service;

import com.buguagaoshu.food.enums.VerificationCodeScene;
import com.buguagaoshu.food.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于进程内存的验证码服务实现
 * <p>
 * 使用 {@link ConcurrentHashMap} 存储，key = 场景 + 目标（手机号/邮箱），value 携带验证码、发送时间与过期时间。
 * 目标是否包含 {@code @} 决定走邮件通道还是短信通道。过期条目在访问时惰性清理，无需额外定时任务。
 * 单机测试足够；多实例部署时请替换为基于 Redis 的实现（接口保持不变）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Service
public class InMemoryVerificationCodeService implements VerificationCodeService {

    /** key = scene.code + ":" + target */
    private final ConcurrentHashMap<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    private final SmsSender smsSender;

    private final EmailCodeSender emailCodeSender;

    /** 验证码有效期（秒） */
    private final long expireSeconds;

    /** 同一目标同场景最短重发间隔（秒） */
    private final long resendIntervalSeconds;

    /** 验证码位数 */
    private final int codeLength;

    public InMemoryVerificationCodeService(
            SmsSender smsSender,
            EmailCodeSender emailCodeSender,
            @Value("${food.verify-code.expire-seconds:300}") long expireSeconds,
            @Value("${food.verify-code.resend-interval-seconds:60}") long resendIntervalSeconds,
            @Value("${food.verify-code.length:6}") int codeLength) {
        this.smsSender = smsSender;
        this.emailCodeSender = emailCodeSender;
        this.expireSeconds = expireSeconds;
        this.resendIntervalSeconds = resendIntervalSeconds;
        this.codeLength = codeLength;
    }

    @Override
    public void sendCode(String target, VerificationCodeScene scene) {
        String normalized = normalizeTarget(target);
        String key = buildKey(scene, normalized);
        long now = System.currentTimeMillis();

        CodeEntry existing = codeStore.get(key);
        if (existing != null) {
            // 已过期则直接清理，允许重发
            if (existing.isExpired(now)) {
                codeStore.remove(key);
            } else if (now - existing.sentAt < resendIntervalSeconds * 1000) {
                long wait = (resendIntervalSeconds * 1000 - (now - existing.sentAt) + 999) / 1000;
                throw new BusinessException(400, "验证码发送过于频繁，请 " + wait + " 秒后再试");
            }
        }

        String code = generateNumericCode();
        codeStore.put(key, new CodeEntry(code, now, now + expireSeconds * 1000));
        // 邮箱走邮件通道，其余走短信通道（均为可替换实现，当前为日志通道）
        if (isEmail(normalized)) {
            emailCodeSender.send(normalized, code, scene);
        } else {
            smsSender.send(normalized, code, scene);
        }
    }

    @Override
    public void verifyCode(String target, String code, VerificationCodeScene scene) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        String normalized = normalizeTarget(target);
        String key = buildKey(scene, normalized);
        CodeEntry entry = codeStore.get(key);
        long now = System.currentTimeMillis();

        if (entry == null) {
            throw new BusinessException(400, "请先获取验证码");
        }
        if (entry.isExpired(now)) {
            codeStore.remove(key);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    @Override
    public void consumeCode(String target, VerificationCodeScene scene) {
        codeStore.remove(buildKey(scene, normalizeTarget(target)));
    }

    private String buildKey(VerificationCodeScene scene, String target) {
        return scene.getCode() + ":" + target;
    }

    /** 校验并归一化目标：去除首尾空白，非空返回，空则抛出业务异常 */
    private String normalizeTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new BusinessException(400, "手机号或邮箱不能为空");
        }
        return target.trim();
    }

    /** 依据是否包含 @ 判定邮箱 */
    private boolean isEmail(String target) {
        return target.contains("@");
    }

    /** 生成指定位数的纯数字验证码 */
    private String generateNumericCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /** 验证码条目 */
    private static class CodeEntry {
        private final String code;
        private final long sentAt;
        private final long expireAt;

        CodeEntry(String code, long sentAt, long expireAt) {
            this.code = code;
            this.sentAt = sentAt;
            this.expireAt = expireAt;
        }

        boolean isExpired(long now) {
            return now > expireAt;
        }
    }
}
