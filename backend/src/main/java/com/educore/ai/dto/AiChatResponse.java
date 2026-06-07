package com.educore.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * رد الـ AI service على سؤال الطالب.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResponse {

    private String       question;
    private String       answer;
    private List<Source> sources;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String source;
        private Object page;
        private Double score;
        private String preview;
    }
}
