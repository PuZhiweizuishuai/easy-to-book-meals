package com.buguagaoshu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buguagaoshu.food.enums.ReturnCodeEnum;

@Data
public class ResponseDetails extends HashMap<String, Object> {
      private ResponseDetails() {
        put("status", ReturnCodeEnum.SUCCESS.getCode());
        put("message", ReturnCodeEnum.SUCCESS.getMsg());
        put("timestamp", LocalDateTime.now());
    }

    public static ResponseDetails ok() {
        return new ResponseDetails();
    }

    public static ResponseDetails ok(Integer code, String msg) {
        ResponseDetails r = new ResponseDetails();
        r.put("status", code);
        r.put("message", msg);
        return r;
    }

    public static ResponseDetails ok(String msg) {
        ResponseDetails r = new ResponseDetails();
        r.put("message", msg);
        return r;
    }

    public static ResponseDetails ok(ReturnCodeEnum codeEnum) {
        ResponseDetails r = new ResponseDetails();
        r.put("status", codeEnum.getCode());
        r.put("message", codeEnum.getMsg());
        return r;
    }

    public static ResponseDetails ok(Map<String, Object> map) {
        ResponseDetails r = new ResponseDetails();
        r.putAll(map);
        return r;
    }

    public static ResponseDetails ok(List<?> data) {
        ResponseDetails r = new ResponseDetails();
        r.put("data", data);
        return r;
    }

    @Override
    public ResponseDetails put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}