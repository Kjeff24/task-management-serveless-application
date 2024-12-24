package org.example.mapper;

import org.example.dto.TaskRequest;
import org.example.model.Task;
import org.springframework.stereotype.Service;

@Service
public class TaskMapper {

    public Task toTask(TaskRequest taskRequest, String adminId) {
        return Task.builder()
//                .taskId(UUID.randomUUID().toString())
                .title(taskRequest.title())
                .description(taskRequest.description())
                .assignedTo(taskRequest.assignedTo())
                .status(taskRequest.status())
                .deadline(taskRequest.deadline())
                .createdBy(adminId)
                .hasSentDeadlineNotification(0)
                .build();
    }
}
