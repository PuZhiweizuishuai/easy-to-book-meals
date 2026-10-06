package com.buguagaoshu.food.enums;

/**
 * 验证码使用场景
 * <p>
 * 不同场景的验证码相互独立：同一手机号在不同场景下分别生成、分别校验，
 * 避免「修改手机号」时新旧号码的验证码串用。
 * <p>
 * 后续接入真实短信服务时，可按场景映射不同的短信模板 code / 签名。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
public enum VerificationCodeScene {

    /**
     * 修改手机号：校验当前已绑定的手机号（证明是本人操作）
     */
    CHANGE_PHONE_OLD("change_phone_old", "修改手机号-原手机号校验"),

    /**
     * 修改手机号：校验新手机号（证明新号码本人可接收）
     */
    CHANGE_PHONE_NEW("change_phone_new", "修改手机号-新手机号校验"),

    /**
     * 用户注册：校验注册手机号或邮箱（证明联系方式本人可接收）
     * <p>
     * 手机号注册与邮箱注册共用本场景，验证码按「场景 + 目标号码/邮箱」分别存储，互不干扰。
     */
    REGISTER("register", "用户注册-手机号/邮箱校验");

    private final String code;

    private final String description;

    VerificationCodeScene(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据场景编码解析枚举，非法编码抛出 IllegalArgumentException
     */
    public static VerificationCodeScene fromCode(String code) {
        for (VerificationCodeScene scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        throw new IllegalArgumentException("不支持的验证码场景: " + code);
    }
}
