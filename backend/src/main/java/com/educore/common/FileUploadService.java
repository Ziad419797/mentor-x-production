package com.educore.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    @Value("${file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${cloudinary.folder.profiles:profiles}")
    private String profilesFolder;

    @Value("${cloudinary.folder.certificates:certificates}")
    private String certificatesFolder;

    @Value("${cloudinary.folder.documents:documents}")
    private String documentsFolder;

    // تعريف أنواع الملفات المسموح بها لكل مجلد
    private static final Map<String, List<String>> FOLDER_ALLOWED_TYPES = Map.of(
            "profiles", Arrays.asList("image/jpeg", "image/jpg", "image/png"),
            "certificates", Arrays.asList("image/jpeg", "image/jpg", "image/png", "application/pdf"),
            "documents", Arrays.asList("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "images", Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif")
    );

    /**
     * رفع ملف مع تحديد المجلد ونوع الملف المسموح به
     */
    public String uploadFile(MultipartFile file, String folder, String fileType) {
        validateFile(file, folder, fileType);
        return uploadToCloudinary(file, folder);
    }

    /**
     * رفع صورة شخصية
     */
    public String uploadProfilePicture(MultipartFile file) {
        return uploadFile(file, profilesFolder, "profile");
    }

    /**
     * رفع شهادة ميلاد
     */
    public String uploadBirthCertificate(MultipartFile file) {
        return uploadFile(file, certificatesFolder, "certificate");
    }

    /**
     * رفع مستند عام
     */
    public String uploadDocument(MultipartFile file, String documentType) {
        return uploadFile(file, documentsFolder, documentType);
    }

    /**
     * رفع صورة عامة
     */
    public String uploadImage(MultipartFile file, String imageType) {
        return uploadFile(file, "images", imageType);
    }

    /**
     * رفع ملف مع تحديد المجلد
     */
    private String uploadToCloudinary(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = folder + "/" + UUID.randomUUID() + extension;

            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "public_id", uniqueFileName.replace(".", "_"),
                    "folder", folder,
                    "overwrite", false,
                    "resource_type", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            String url = uploadResult.get("secure_url").toString();

            log.info("File uploaded successfully: {} -> {}", originalFilename, url);
            return url;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("فشل في رفع الملف: " + e.getMessage());
        }
    }

    /**
     * التحقق من الملف
     */
    private void validateFile(MultipartFile file, String folder, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("الملف مطلوب");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("حجم الملف يجب أن لا يتجاوز " + (maxFileSize / 1024 / 1024) + " ميجابايت");
        }

        String contentType = file.getContentType();
        List<String> allowedTypes = FOLDER_ALLOWED_TYPES.get(folder);

        if (allowedTypes != null && (contentType == null || !allowedTypes.contains(contentType))) {
            throw new IllegalArgumentException("نوع الملف غير مسموح به للمجلد: " + folder);
        }
    }

    /**
     * حذف ملف
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                String publicId = extractPublicIdFromUrl(fileUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    log.info("File deleted successfully: {}", fileUrl);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }

    /**
     * حذف عدة ملفات دفعة واحدة
     */
    public void deleteFiles(List<String> fileUrls) {
        fileUrls.parallelStream().forEach(this::deleteFile);
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            String fileName = parts[parts.length - 1];
            String folder = parts[parts.length - 2];
            return folder + "/" + fileName.substring(0, fileName.lastIndexOf("."));
        } catch (Exception e) {
            log.warn("Could not extract public_id from URL: {}", url);
            return null;
        }
    }

}