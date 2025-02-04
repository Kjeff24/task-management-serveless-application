package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignToRequest(
        @NotBlank(message = "taskId field is required")
        String taskId,
        @NotBlank(message = "email field is required")
        String assignedTo) {
}
