package com.educore.exception;

/**
 * Thrown when a JWT token is malformed, has an invalid signature,
 * or is used in the wrong context (e.g., using a REFRESH token as an ACCESS token).
 * Maps to HTTP 401 Unauthorized in GlobalExceptionHandler.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
