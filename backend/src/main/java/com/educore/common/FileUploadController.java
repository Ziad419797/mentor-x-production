package com.educore.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * مسؤول عن استقبال رفع الملفات/الصور (الصورة الشخصية، صورة البطاقة، ...)
 * من لوحة تحكم المعلم وتطبيق الطالب — يستخدمها مثلاً نموذج "تعديل بيانات الطالب".
 * كان الكلاس ده متعطّل (مُعلّق بالكامل) فكان أي رفع صورة بيفشل بـ 404.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<GlobalResponse<String>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "general") String type) {
        try {
            String url = fileUploadService.uploadImage(file, type);
            return ResponseEntity.ok(GlobalResponse.<String>builder()
                    .success(true)
                    .message("تم رفع الملف بنجاح")
                    .data(url)
                    .build());
        } catch (IllegalArgumentException e) {
            // أخطاء التحقق من الملف (نوع/حجم غير مسموح) — خطأ من المستخدم وليس عطل في السيرفر
            log.warn("File upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(GlobalResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError().body(GlobalResponse.<String>builder()
                    .success(false)
                    .message("فشل رفع الصورة: " + e.getMessage())
                    .build());
        }
    }
}
