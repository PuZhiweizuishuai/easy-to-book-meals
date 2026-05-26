package com.buguagaoshu.food.exception;

import com.buguagaoshu.food.enums.ReturnCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ReturnCodeEnum.FAIL.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ReturnCodeEnum returnCodeEnum) {
        super(returnCodeEnum.getMsg());
        this.code = returnCodeEnum.getCode();
    }

}
