package com.buguagaoshu.food.enums;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 * 计价单位
 */
public enum PriceType {
    FIXED(0, "份"),
    WEIGHT_BASED_G(1, "克"),
    WEIGHT_BASED_CATTY(2, "斤")
    ;


    public int code;
    public String msg;

    PriceType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static String getMsg(int code) {
        for (PriceType type : PriceType.values()) {
            if (type.code == code) {
                return type.msg;
            }
        }
        // 默认返回份
        return "份";
    }
}
