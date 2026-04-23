package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends DispatchSimException {

    public BusinessException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
