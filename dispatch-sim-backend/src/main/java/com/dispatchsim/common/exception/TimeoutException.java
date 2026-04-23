package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class TimeoutException extends DispatchSimException {

    public TimeoutException(String message) {
        super(HttpStatus.GATEWAY_TIMEOUT, message);
    }
}
