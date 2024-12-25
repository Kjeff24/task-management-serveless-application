package org.example.dto;

import java.time.LocalDateTime;

public record TaskRequest(
        String name,
        String description,
        String assignedTo,
        LocalDateTime deadline
//        String status

) {
}
