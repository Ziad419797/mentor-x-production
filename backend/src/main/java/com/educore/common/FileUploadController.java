//package com.educore.common;
//
//
//import com.educore.common.FileUploadService ;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/api/files")
//@RequiredArgsConstructor
//public class FileUploadController {
//
//    private final FileUploadService fileUploadService;
//
//    // التعديل هنا: ضفنا الـ consumes عشان السواجر يفهم إنه ملف مش JSON
//    @PostMapping(value = "/upload", consumes = "multipart/form-data")
//    public ResponseEntity<String> uploadImage(@RequestPart("file") MultipartFile file) {
//        try {
//            String imageUrl = fileUploadService.uploadFile(file);
//            return ResponseEntity.ok(imageUrl);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("فشل رفع الصورة: " + e.getMessage());
//        }
//    }
//}
