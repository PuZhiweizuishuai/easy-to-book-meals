package com.buguagaoshu.food.utils;


import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.text.SimpleDateFormat;

public class JsonUtils {
    // 全局单例 JsonMapper，Jackson 3 中推荐使用 JsonMapper 代替 ObjectMapper
    private static final JsonMapper JSON_MAPPER;

    // 静态代码块通过 Builder 模式进行全局配置
    static {
        JSON_MAPPER = JsonMapper.builder()
                // 忽略在JSON字符串中存在，但在Java对象中不存在的属性
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 忽略空对象转JSON时的报错
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 取消将日期时间转换为时间戳，改为指定格式输出
                //.disable(SerializationFeature.wr)
                // 设置日期时间格式
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .build(); // 构建不可变的 JsonMapper 实例
    }

    // 私有化构造器，禁止外部实例化
    private JsonUtils() {}

    /**
     * 将 Java 对象转换为 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("对象转JSON字符串失败", e);
        }
    }

    /**
     * 将 JSON 字符串转换为指定的 Java 对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * 将 JSON 字符串转换为复杂的泛型对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("JSON字符串转泛型对象失败", e);
        }
    }
}
