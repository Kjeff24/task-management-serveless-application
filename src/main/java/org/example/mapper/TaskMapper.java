package org.example.mapper;

import org.example.dto.TaskRequest;
import org.example.enums.TaskStatus;
import org.example.model.Task;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class TaskMapper {

    public Task toTask(TaskRequest taskRequest, String adminId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        String formattedDeadline = taskRequest.deadline() != null
                ? taskRequest.deadline().format(formatter)
                : null;
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .name(taskRequest.name())
                .description(taskRequest.description())
                .assignedTo(taskRequest.assignedTo())
                .status(TaskStatus.open.toString())
                .deadline(formattedDeadline)
                .hasSentDeadlineNotification(0)
                .createdBy(adminId)
                .build();
    }
}
