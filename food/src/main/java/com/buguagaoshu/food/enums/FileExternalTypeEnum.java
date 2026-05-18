package com.buguagaoshu.food.enums;



/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2020-09-06 14:04
 */
public enum FileExternalTypeEnum {
    /**
     * 文件类型
     */
    STOREFRONT(0, "店面关联文件"),
    FOOD(1, "食品关联文件"),
    ;
    final int code;
    final String msg;


    FileExternalTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
