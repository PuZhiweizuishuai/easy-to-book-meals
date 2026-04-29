package com.buguagaoshu.food.exception;

import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseDetails handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return ResponseDetails.ok(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseDetails handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("参数异常: {}", e.getMessage());
        return ResponseDetails.ok(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseDetails handleException(Exception e) {
        logger.error("系统异常: ", e);
        return ResponseDetails.ok(ReturnCodeEnum.FAIL.getCode(), "系统内部错误");
    }
}
