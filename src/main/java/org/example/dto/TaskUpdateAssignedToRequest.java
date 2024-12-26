package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskUpdateAssignedToRequest(
        @NotBlank(message = "taskId field is required")
        String taskId,
        @NotBlank(message = "assignedTo field is required")
        String assignedTo
) {
}
