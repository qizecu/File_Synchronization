package com.example.syncmanager.common;

import lombok.Getter;

/**
 * 业务异常，Controller 层统一由全局异常处理器捕获
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
