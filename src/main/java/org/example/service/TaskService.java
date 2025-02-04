package org.example.service;

import org.example.dto.*;
import org.example.model.Task;

import java.util.List;

public interface TaskService {
    Task createTask(TaskRequest task, String adminEmail);

    Task assignTask(TaskUpdateAssignedToRequest request);

    Task updateTaskStatus(TaskUpdateStatusRequest request);

    TaskResponse getAllTasks();

    TaskResponse getTasksForUser(String userEmail);

    Task getTaskById(String taskId);

    Task addUserComment(UserCommentRequest request);

    Task updateTask(TaskRequest task,  String taskId);

    void deleteTask(String taskId);

    Task changeAssignedToUser(AssignToRequest request);
}
