package org.example.service;

import org.example.dto.TaskRequest;
import org.example.model.Task;

import java.util.List;

public interface TaskService {
    Task createTask(TaskRequest task, String adminEmail);

    Task assignTask(String taskId, String userEmail);

    Task updateTaskStatus(String taskId, String status);

    List<Task> getAllTasks();

    List<Task> getTasksForUser(String userEmail);

    Task getTaskById(String taskId);
}
