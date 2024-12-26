package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
import org.example.dto.TaskUpdateAssignedToRequest;
import org.example.dto.TaskUpdateStatusRequest;
import org.example.exception.NotFoundException;
import org.example.mapper.TaskMapper;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.example.service.SqsService;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final SqsService sqsService;
    @Value("${app.aws.sns.topics.tasks.assignment.arn}")
    private String tasksAssignmentTopicArn;
    @Value("${app.aws.sns.topics.tasks.reopened.arn}")
    private String reopenedTasksTopicArn;

    public Task createTask(TaskRequest taskRequest, String adminEmail) {
        Task task = taskMapper.toTask(taskRequest, adminEmail);
        System.out.println(task.toString());
        taskRepository.saveTask(task);
        sqsService.sendToSQS(task, "TASK ASSIGNMENT NOTIFICATION", "New task created has been assigned to you", tasksAssignmentTopicArn);
        return task;
    }

    public Task assignTask(TaskUpdateAssignedToRequest request) {
        Task task = taskRepository.updateAssignedTo(request);
        sqsService.sendToSQS(task, "TASK RE-ASSIGNMENT NOTIFICATION","Task has been re-assigned to you", tasksAssignmentTopicArn);
        return task;
    }

    public Task updateTaskStatus(TaskUpdateStatusRequest request) {
        Task task = taskRepository.updateTaskStatus(request);
        if (request.status().equals("open")) {
            sqsService.sendToSQS(task, "TASK RE-OPEN NOTIFICATION", "Task has been re-opened, ensure you complete it", reopenedTasksTopicArn);
        }
        return task;
    }

    public List<Task> getAllTasks() {
        return taskRepository.getAllTasks();
    }

    public List<Task> getTasksForUser(String userEmail) {
        System.out.println("looking up tasks for user " + userEmail);
        List<Task> tasks = taskRepository.getTasksByAssignedTo(userEmail);
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
