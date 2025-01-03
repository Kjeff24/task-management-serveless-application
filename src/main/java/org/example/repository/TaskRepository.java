package org.example.repository;

import org.example.dto.TaskUpdateAssignedToRequest;
import org.example.dto.TaskUpdateStatusRequest;
import org.example.dto.UserCommentRequest;
import org.example.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    void saveTask(Task task);

    Task updateAssignedTo(TaskUpdateAssignedToRequest request);

    Task updateTaskStatus(TaskUpdateStatusRequest request);

    List<Task> findAllTasks();

    List<Task> findAllTasksByAssignedTo(String userId);

    Optional<Task> findByTaskId(String taskId);

    Task setComment(UserCommentRequest request);
}
