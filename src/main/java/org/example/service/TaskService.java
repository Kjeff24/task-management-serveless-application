package org.example.service;

import org.example.dto.TaskRequest;
import org.example.dto.TaskUpdateAssignedToRequest;
import org.example.dto.TaskUpdateStatusRequest;
import org.example.dto.UserCommentRequest;
import org.example.model.Task;

import java.util.List;

public interface TaskService {
    Task createTask(TaskRequest task, String adminEmail);

    Task assignTask(TaskUpdateAssignedToRequest request);

    Task updateTaskStatus(TaskUpdateStatusRequest request);

    List<Task> getAllTasks();

    List<Task> getTasksForUser(String userEmail);

    Task getTaskById(String taskId);

    Task addUserComment(UserCommentRequest request);
}
