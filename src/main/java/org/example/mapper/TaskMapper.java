package org.example.mapper;

import org.example.dto.TaskRequest;
import org.example.model.Task;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class TaskMapper {

    public Task toTask(TaskRequest taskRequest, String adminId) {
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        // Format the LocalDateTime as a string
        String formattedDeadline = taskRequest.deadline() != null
                ? taskRequest.deadline().format(formatter)
                : null;
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .name(taskRequest.name())
                .description(taskRequest.description())
                .assignedTo(taskRequest.assignedTo())
                .status("open")
                .deadline(formattedDeadline)
                .hasSentDeadlineNotification(0)
                .createdBy(adminId)
                .build();
    }
}
