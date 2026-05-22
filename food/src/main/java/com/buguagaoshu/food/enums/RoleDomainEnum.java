package com.buguagaoshu.food.enums;

public enum RoleDomainEnum {
    GLOBAL("GLOBAL", "全局域"),
    STORE("STORE", "门店域");

    private final String code;
    private final String description;

    RoleDomainEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
