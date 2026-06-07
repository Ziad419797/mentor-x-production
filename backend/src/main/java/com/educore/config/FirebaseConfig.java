package com.educore.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * تهيئة Firebase Admin SDK.
 *
 * طرق الـ Configuration:
 *  1. ملف JSON في الـ classpath (src/main/resources/firebase-service-account.json)
 *  2. Environment variable: GOOGLE_APPLICATION_CREDENTIALS (path لملف JSON)
 *
 * في الـ production: استخدم env variable لتجنب وضع credentials في الكود.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    /**
     * اسم ملف الـ service account — محفوظ في src/main/resources.
     * لو مش موجود، بيحاول يستخدم GOOGLE_APPLICATION_CREDENTIALS تلقائياً.
     */
    @Value("${firebase.service-account-file:firebase-service-account.json}")
    private String serviceAccountFile;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initializeFirebase() {
        if (!firebaseEnabled) {
            log.info("Firebase disabled via config (firebase.enabled=false)");
            return;
        }

        // تجنب التهيئة المزدوجة لو الـ app موجود بالفعل
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized");
            return;
        }

        try {
            // محاولة 1: ملف JSON في الـ classpath
            ClassPathResource resource = new ClassPathResource(serviceAccountFile);
            if (resource.exists()) {
                InputStream serviceAccount = resource.getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized from classpath file: {}", serviceAccountFile);
                return;
            }

            // محاولة 2: GOOGLE_APPLICATION_CREDENTIALS env variable
            String googleCredsEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (googleCredsEnv != null && !googleCredsEnv.isBlank()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized from GOOGLE_APPLICATION_CREDENTIALS env");
                return;
            }

            log.warn("Firebase not initialized: no service account file found at '{}' and GOOGLE_APPLICATION_CREDENTIALS not set. " +
                     "Push notifications will be disabled. " +
                     "To enable: add firebase-service-account.json to src/main/resources/", serviceAccountFile);

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}. Push notifications will be disabled.", e.getMessage());
        }
    }
}
