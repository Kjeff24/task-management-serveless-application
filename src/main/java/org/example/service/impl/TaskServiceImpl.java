package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
import org.example.mapper.TaskMapper;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
//    private final SqsService sqsService;

    @Value("${app.aws.dynamodb.task.table}")
    private String taskTableName;

    public Task createTask(TaskRequest taskRequest, String adminId) {
        Task task = taskMapper.toTask(taskRequest, adminId);
        System.out.println(task);
        taskRepository.saveTask(task);
//        sqsService.sendToSQS(task);
        return task;
    }

    public Task assignTask(String taskId, String userId) {
        return taskRepository.updateAssignedTo(taskId, userId);
    }

    public Task updateTaskStatus(String taskId, String status) {
        return taskRepository.updateTaskStatus(taskId, status);
    }

    public List<Task> getAllTasks() {
        return taskRepository.getAllTasks();
    }

    public List<Task> getTasksForUser(String userId) {
        return taskRepository.getTasksByAssignedTo(userId);
    }


}
