package org.example.service;

import org.example.dto.TaskRequest;

public interface TaskService {
    void createTask(TaskRequest task, String adminId);
}
