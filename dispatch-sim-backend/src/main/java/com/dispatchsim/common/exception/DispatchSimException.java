package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class DispatchSimException extends RuntimeException {

    private final HttpStatus httpStatus;

    public DispatchSimException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
