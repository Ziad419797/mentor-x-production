package com.educore.exception;

public class FileUploadException extends RuntimeException {
    private final String fileType;
    private final String fileName;

    public FileUploadException(String message, String fileType, String fileName) {
        super(message);
        this.fileType = fileType;
        this.fileName = fileName;
    }

    public FileUploadException(String message, String fileType, String fileName, Throwable cause) {
        super(message, cause);
        this.fileType = fileType;
        this.fileName = fileName;
    }
}