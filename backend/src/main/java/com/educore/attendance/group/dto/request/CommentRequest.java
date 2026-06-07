package com.educore.attendance.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommentRequest {

    @NotBlank(message = "التعليق لا يمكن أن يكون فارغاً")
    @Size(max = 1000)
    private String comment;
}
