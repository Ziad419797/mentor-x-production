package com.educore.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentProfileResponse {

    private Long   id;
    private String phone;
    private String name;
    private String profileImageUrl;

    /** أسماء الطلاب المرتبطين بولي الأمر */
    private List<StudentSummary> students;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long   id;
        private String fullName;
        private String studentCode;
        private String grade;
    }
}
