package org.example.service;

import org.example.dto.TaskRequest;
import org.example.model.Task;

import java.util.List;

public interface TaskService {
    Task createTask(TaskRequest task, String adminId);

    Task assignTask(String taskId, String userId);

    Task updateTaskStatus(String taskId, String status);

    List<Task> getAllTasks();

    List<Task> getTasksForUser(String userId);

    Task getTaskById(String taskId);
}
