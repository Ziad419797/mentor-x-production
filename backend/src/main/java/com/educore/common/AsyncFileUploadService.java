package com.educore.common;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncFileUploadService {

    private final FileUploadService fileUploadService;

    public AsyncFileUploadService(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Async
    public CompletableFuture<String> uploadFileAsync(MultipartFile file, String folder, String fileType) {
        return CompletableFuture.completedFuture(
                fileUploadService.uploadFile(file, folder, fileType)
        );
    }

    @Async
    public CompletableFuture<Void> deleteFileAsync(String fileUrl) {
        fileUploadService.deleteFile(fileUrl);
        return CompletableFuture.completedFuture(null);
    }
}