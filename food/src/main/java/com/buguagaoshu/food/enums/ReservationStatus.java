package com.buguagaoshu.food.enums;



import lombok.Getter;

@Getter
public enum ReservationStatus {
    PENDING("待确认", 0),       // 客户刚提交，等商家确认（视业务情况可省略）
    CONFIRMED("已确认", 1),     // 商家已确认预定
    SEATED("已到店", 2),        // 客户到店落座，预定转化为实际就餐
    CANCELLED("已取消", 3),     // 客户取消或商家取消
    NO_SHOW("未到店", 4),       // 过了预定时间未到店
    COMPLETED("已完成", 5);     // 就餐结束

    private final String description;

    private final int code;

    ReservationStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }
}
