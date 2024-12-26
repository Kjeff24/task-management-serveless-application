package org.example.dto;

public record TaskUpdateAssignedToRequest(
        String taskId,
        String assignedTo
) {
}
