package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
        @NotBlank(message = "email field is required")
        String email,
        @NotBlank(message = "fullName field is required")
        String fullName,
        @Pattern(regexp = "ADMIN|USER", message = "status should be an ADMIN or USER")
        String role) {
}
