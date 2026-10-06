package com.buguagaoshu.food.service;

import com.buguagaoshu.food.enums.VerificationCodeScene;

/**
 * 短信发送通道抽象（策略接口）
 * <p>
 * 当前只有 {@link LogSmsSender} 测试实现（把验证码打印到后端日志）。
 * 后续接入真实短信服务时，新增一个实现本接口的类即可，例如：
 * <pre>
 *   &#64;Component
 *   &#64;ConditionalOnProperty(prefix = "food.sms", name = "provider", havingValue = "aliyun")
 *   public class AliyunSmsSender implements SmsSender { ... }
 * </pre>
 * 然后在 application.yml 中把 {@code food.sms.provider} 切换为 {@code aliyun}，
 * 业务代码（{@link VerificationCodeService}）无需任何改动。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
public interface SmsSender {

    /**
     * 发送一条验证码短信
     *
     * @param phone 接收手机号
     * @param code  验证码
     * @param scene 使用场景（真实短信服务可据此选择模板 / 签名）
     */
    void send(String phone, String code, VerificationCodeScene scene);

    /**
     * 当前通道标识，如 log / aliyun / tencent
     */
    String channel();
}
