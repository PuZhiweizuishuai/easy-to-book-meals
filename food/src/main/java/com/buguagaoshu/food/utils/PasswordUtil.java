package com.buguagaoshu.food.utils;

import cn.dev33.satoken.secure.BCrypt;

/**
 * 密码加密工具（BCrypt 加盐哈希）
 * <p>
 * 统一使用 BCrypt 替代无盐 MD5；同时兼容历史 MD5 密文的校验，
 * 登录成功后由调用方透明升级为 BCrypt。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
public class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * 生成 BCrypt 密文（自动加盐）
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    /**
     * 判断密文是否为 BCrypt 格式
     */
    public static boolean isBCrypt(String hashed) {
        return hashed != null && hashed.startsWith("$2");
    }

    /**
     * 校验密码。
     * BCrypt 密文走 BCrypt 校验；历史 MD5 密文回退到 MD5 校验（校验通过后应升级为 BCrypt）。
     */
    public static boolean matches(String rawPassword, String hashed) {
        if (rawPassword == null || hashed == null) {
            return false;
        }
        if (isBCrypt(hashed)) {
            return BCrypt.checkpw(rawPassword, hashed);
        }
        return MD5Util.getMD5(rawPassword).equals(hashed);
    }
}
