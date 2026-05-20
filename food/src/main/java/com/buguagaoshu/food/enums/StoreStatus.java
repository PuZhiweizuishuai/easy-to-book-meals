package com.buguagaoshu.food.enums;

import lombok.Getter;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
public enum StoreStatus {
    PENDING_REVIEW(-1, "新店待审核"),
    OPEN(0, "营业中"),      // 营业中
    CLOSED(1, "休息中"),    // 休息中
    DISABLED(2, "已停业")   // 已停业
    ;
    final int code;
    final String name;

    StoreStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

}
