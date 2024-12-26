package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank(message = "email field is required")
        String email,
        @NotBlank(message = "fullName field is required")
        String fullName) {
}
