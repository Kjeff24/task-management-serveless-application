package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
import org.example.exception.NotFoundException;
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
        System.out.println(task.toString());
        taskRepository.saveTask(task);
//        sqsService.sendToSQS(task);
        return task;
    }

    public Task assignTask(String taskId, String userId) {
        Task task = taskRepository.updateAssignedTo(taskId, userId);
        System.out.println(task.toString());
        return task;
    }

    public Task updateTaskStatus(String taskId, String status) {
        Task task = taskRepository.updateTaskStatus(taskId, status);
        System.out.println(task.toString());
        return task;
    }

    public List<Task> getAllTasks() {
        return taskRepository.getAllTasks();
    }

    public List<Task> getTasksForUser(String userId) {
        System.out.println("looking up tasks for user " + userId);
        List<Task> tasks = taskRepository.getTasksByAssignedTo(userId);
        System.out.println("found " + tasks.size() + " tasks" + tasks);
        for (Task task : tasks) {
            System.out.println(task.toString());
        }
        return tasks;
    }

    public Task getTaskById(String taskId) {
        System.out.println("Looking up task with id " + taskId);
        Task task = taskRepository.getTaskById(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        System.out.println(task.toString());
        return task;
    }


}
