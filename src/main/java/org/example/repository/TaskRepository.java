package org.example.repository;

import org.example.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    void saveTask(Task task);

    Task updateAssignedTo(String taskId, String userId);

    Task updateTaskStatus(String taskId, String status);

    List<Task> getAllTasks();

    List<Task> getTasksByAssignedTo(String userId);

    Optional<Task> getTaskById(String taskId);
}
