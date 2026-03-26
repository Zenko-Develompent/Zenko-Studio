package com.hackathon.edu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
    }
}
