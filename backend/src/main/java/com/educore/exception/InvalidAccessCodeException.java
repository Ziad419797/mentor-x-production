package com.educore.exception;

public class InvalidAccessCodeException extends RuntimeException {
    public InvalidAccessCodeException(String message) {
        super(message);
    }
}
