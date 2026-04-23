package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends DispatchSimException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
