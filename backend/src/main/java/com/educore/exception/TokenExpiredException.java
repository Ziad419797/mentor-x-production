package com.educore.exception;

/**
 * Thrown when a JWT token has passed its expiration time.
 * Maps to HTTP 401 Unauthorized in GlobalExceptionHandler.
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
