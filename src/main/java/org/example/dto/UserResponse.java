package org.example.dto;

import lombok.Builder;

@Builder
public record UserResponse(String userId, String email, String fullName) {
}
