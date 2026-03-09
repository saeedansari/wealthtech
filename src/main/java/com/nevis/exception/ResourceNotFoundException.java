package com.nevis.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ValidationException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Resource Not Found", message);
    }
}
