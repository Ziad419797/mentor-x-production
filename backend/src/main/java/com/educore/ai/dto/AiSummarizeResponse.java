package com.educore.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * رد الـ AI service على طلب التلخيص.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSummarizeResponse {

    @JsonProperty("lesson_id")
    private Long lessonId;

    private String language;
    private String summary;
}
