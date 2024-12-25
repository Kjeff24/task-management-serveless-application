package org.example.mapper;

import org.example.dto.TaskRequest;
import org.example.model.Task;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskMapper {

    public Task toTask(TaskRequest taskRequest, String adminId) {
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .name(taskRequest.name())
                .description(taskRequest.description())
                .assignedTo(taskRequest.assignedTo())
                .status("open")
                .deadline(taskRequest.deadline())
                .hasSentDeadlineNotification(0)
                .createdBy(adminId)
                .build();
    }
}
