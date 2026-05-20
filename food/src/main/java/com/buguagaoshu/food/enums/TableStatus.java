package com.buguagaoshu.food.enums;


import lombok.Getter;

@Getter
public enum TableStatus {
    FREE("空闲", 0),           // 可预订/可就餐
    OCCUPIED("已占用", 1),     // 正在就餐中
    RESERVED("已预订", 2),     // 被预订等待就餐
    CLEANING("清理中", 3),     // 客人离席，等待服务员清理
    DISABLED("停用", 4);       // 餐桌损坏或暂不使用

    private final int code;

    private final String description;

    TableStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }
}
