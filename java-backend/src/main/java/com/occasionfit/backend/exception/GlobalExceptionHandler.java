package com.occasionfit.backend.exception;

import com.occasionfit.backend.dto.res.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TokenLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenLimit(TokenLimitExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)   // 429
                .body(new ApiErrorResponse(ex.getMessage(), "TOKEN_LIMIT_EXCEEDED"));
    }
}