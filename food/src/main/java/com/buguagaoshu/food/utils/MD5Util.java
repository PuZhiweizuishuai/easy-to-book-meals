package com.buguagaoshu.food.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-08
 */
public class MD5Util {
    public static String getMD5(String input) {
        try {
            // 1. 获取 MD5 算法实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 2. 将字符串转换为字节数组，并明确指定 UTF-8 编码，防止中文乱码
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            // 3. 执行哈希计算
            byte[] hashedBytes = md.digest(inputBytes);

            // 4. 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                // %02x 确保每个字节都转换为两位十六进制数，不足两位前面补0
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // 理论上所有 JDK 都支持 MD5，此异常通常不会发生
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
