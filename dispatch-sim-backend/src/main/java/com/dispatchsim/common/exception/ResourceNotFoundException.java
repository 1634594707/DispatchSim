package com.dispatchsim.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DispatchSimException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
