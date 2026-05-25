package com.buguagaoshu.food.enums;

import lombok.Getter;

@Getter
public enum RoleDomainEnum {
    GLOBAL("GLOBAL", "全局域"),
    STORE("STORE", "门店域");

    private final String code;
    private final String description;

    RoleDomainEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
