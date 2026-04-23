package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class StateException extends DispatchSimException {

    public StateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
