package org.example.repository;

import org.example.dto.*;
import org.example.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    void saveTask(Task task);

    Task updateAssignedTo(TaskUpdateAssignedToRequest request);

    Task updateTaskStatus(TaskUpdateStatusRequest request);

    TaskResponse findAllTasks();

    TaskResponse findAllTasksByAssignedTo(String userId);

    Optional<Task> findByTaskId(String taskId);

    Task setComment(UserCommentRequest request);
}
