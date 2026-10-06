package com.buguagaoshu.food.service;

import com.buguagaoshu.food.enums.VerificationCodeScene;

/**
 * 验证码服务
 * <p>
 * 负责验证码的「生成 → 存储（带过期时间） → 发送 → 校验 → 作废」完整闭环。
 * 目标（target）既可以是手机号，也可以是邮箱：服务内部依据 target 是否包含 {@code @}
 * 自动选择短信通道（{@link SmsSender}）或邮件通道（{@link EmailCodeSender}）。
 * 当前实现 {@link InMemoryVerificationCodeService} 使用进程内存存储，适合单机测试；
 * 后续若多实例部署或接入 Redis，可新增一个基于 Redis 的实现并替换，
 * 上层 Controller 无需改动。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
public interface VerificationCodeService {

    /**
     * 生成验证码并发送到指定目标（手机号或邮箱）
     * <p>
     * 同一目标同一场景存在最短重发间隔，发送过频将抛出业务异常。
     *
     * @param target 接收手机号或邮箱
     * @param scene  使用场景
     */
    void sendCode(String target, VerificationCodeScene scene);

    /**
     * 校验验证码是否正确（仅校验，不作废）
     * <p>
     * 用于需要同时校验多个验证码的场景（如修改手机号时新旧号码都要校验）：
     * 先分别调用本方法确认全部正确，再统一调用 {@link #consumeCode} 作废，
     * 避免「一个验证码已被作废、另一个校验失败」导致用户需重新获取前一个验证码。
     *
     * @param target 手机号或邮箱
     * @param code   用户填写的验证码
     * @param scene  使用场景
     */
    void verifyCode(String target, String code, VerificationCodeScene scene);

    /**
     * 作废指定目标在指定场景下的验证码（校验全部通过、业务提交成功后调用，保证一次性使用）
     *
     * @param target 手机号或邮箱
     * @param scene  使用场景
     */
    void consumeCode(String target, VerificationCodeScene scene);
}
