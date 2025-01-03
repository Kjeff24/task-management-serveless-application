package org.example.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotBlank(message = "name field is required")
        String name,
        @NotBlank(message = "description field is required")
        String description,
        @NotBlank(message = "assignedTo field is required")
        String assignedTo,
        @NotNull(message = "deadline field is required")
        @Future(message = "deadline must be in the future")
        LocalDateTime deadline
) {
}
