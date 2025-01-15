package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record TaskUpdateStatusRequest(
        @NotBlank(message = "taskId field is required")
        String taskId,
        @Pattern(regexp = "open|completed", message = "status should be an open or completed")
        String status,
        LocalDateTime deadline
) {
}
