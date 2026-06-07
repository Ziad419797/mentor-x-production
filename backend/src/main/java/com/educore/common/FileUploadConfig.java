package com.educore.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    private long maxSize = 5242880; // 5MB default
    private Map<String, List<String>> allowedTypes = new HashMap<>();
    private Map<String, String> folders = new HashMap<>();

    // Getters and Setters
}