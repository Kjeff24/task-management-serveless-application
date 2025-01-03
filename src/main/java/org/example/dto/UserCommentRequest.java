package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCommentRequest (
        @NotBlank(message = "taskId field is required")
        String taskId,
        @NotBlank(message = "message field is required")
        String comment
){
}
