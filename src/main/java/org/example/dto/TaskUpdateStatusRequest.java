package org.example.dto;

public record TaskUpdateStatusRequest(
        String taskId,
        String status
) {
}
