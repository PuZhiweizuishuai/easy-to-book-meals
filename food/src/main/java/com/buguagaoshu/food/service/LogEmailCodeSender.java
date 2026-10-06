package com.buguagaoshu.food.service;

import com.buguagaoshu.food.enums.VerificationCodeScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 日志邮件通道（测试用）
 * <p>
 * 在没有真实邮件服务器的阶段，把邮箱验证码以醒目格式打印到后端日志，方便联调测试。
 * 当 application.yml 中 {@code food.email.provider=log}（或缺省不配）时启用本实现。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "food.email", name = "provider", havingValue = "log", matchIfMissing = true)
public class LogEmailCodeSender implements EmailCodeSender {

    @Override
    public void send(String email, String code, VerificationCodeScene scene) {
        log.info("\n" +
                        "================ 邮箱验证码（日志测试通道） ================\n" +
                        "  使用场景 : {} [{}]\n" +
                        "  邮箱     : {}\n" +
                        "  验证码   : {}\n" +
                        "  有效期内有效，正式接入邮件服务后将改为真实邮件下发，不再打印日志\n" +
                        "==========================================================",
                scene.getDescription(), scene.getCode(), email, code);
    }

    @Override
    public String channel() {
        return "log";
    }
}
