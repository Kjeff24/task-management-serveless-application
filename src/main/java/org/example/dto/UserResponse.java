package org.example.dto;

import lombok.Builder;

@Builder
public record UserResponse(String username, boolean enabled) {
}
