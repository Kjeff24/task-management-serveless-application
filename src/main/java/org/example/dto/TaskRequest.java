package org.example.dto;

import java.time.LocalDateTime;

public record TaskRequest(
        String title,
        String description,
        String assignedTo,
        LocalDateTime deadline,
        String status

) {
}
