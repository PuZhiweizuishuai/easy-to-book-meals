package com.buguagaoshu.food.enums;

public enum ReturnCodeEnum {
    SUCCESS(200, "成功"),
    FAIL(500, "失败"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在");

    private final Integer code;
    private final String msg;

    ReturnCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}