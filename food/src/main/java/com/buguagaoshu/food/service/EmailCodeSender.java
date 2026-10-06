package com.buguagaoshu.food.service;

import com.buguagaoshu.food.enums.VerificationCodeScene;

/**
 * 邮件验证码发送通道抽象（策略接口）
 * <p>
 * 当前只有 {@link LogEmailCodeSender} 测试实现（把验证码打印到后端日志）。
 * 后续接入真实邮件服务（SMTP / SendCloud 等）时，新增一个实现本接口的类即可，例如：
 * <pre>
 *   &#64;Component
 *   &#64;ConditionalOnProperty(prefix = "food.email", name = "provider", havingValue = "smtp")
 *   public class SmtpEmailCodeSender implements EmailCodeSender { ... }
 * </pre>
 * 然后在 application.yml 中把 {@code food.email.provider} 切换为 {@code smtp}，
 * 业务代码（{@link VerificationCodeService}）无需任何改动。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
public interface EmailCodeSender {

    /**
     * 发送一封验证码邮件
     *
     * @param email 接收邮箱
     * @param code  验证码
     * @param scene 使用场景（真实邮件服务可据此选择邮件模板）
     */
    void send(String email, String code, VerificationCodeScene scene);

    /**
     * 当前通道标识，如 log / smtp / sendcloud
     */
    String channel();
}
