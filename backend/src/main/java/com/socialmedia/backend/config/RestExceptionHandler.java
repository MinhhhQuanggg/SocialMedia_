package com.socialmedia.backend.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage();
        int status = 400;
        if ("INVALID_CREDENTIALS".equals(msg) || "INVALID_CURRENT_PASSWORD".equals(msg)) status = 401;
        if ("USER_DISABLED".equals(msg)) status = 403;
        if ("LOGIN_REQUIRED".equals(msg) || "PASSWORD_REQUIRED".equals(msg) || "USER_NAME_REQUIRED".equals(msg) 
            || "EMAIL_REQUIRED".equals(msg) || "CURRENT_PASSWORD_REQUIRED".equals(msg) || "NEW_PASSWORD_INVALID".equals(msg)
            || "INVALID_EMAIL".equals(msg) || "INVALID_PHONE".equals(msg) || "INVALID_USER_NAME".equals(msg)
            || "PHONE_TOO_LONG".equals(msg)) status = 400;
        if ("USER_NAME_EXISTS".equals(msg) || "EMAIL_EXISTS".equals(msg)) status = 409;
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
}