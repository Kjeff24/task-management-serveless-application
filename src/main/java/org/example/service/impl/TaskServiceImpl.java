package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.enums.TaskStatus;
import org.example.exception.NotFoundException;
import org.example.mapper.TaskMapper;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.example.service.SqsService;
import org.example.service.TaskService;
import org.example.util.LocalDateTimeConverter;
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
    @Value("${app.aws.sns.topics.tasks.complete.arn}")
    private String taskCompleteTopicArn;

    public Task createTask(TaskRequest taskRequest, String adminEmail) {
        Task task = taskMapper.toTask(taskRequest, adminEmail);
        taskRepository.saveTask(task);
        sqsService.sendToSQS(task, "TASK ASSIGNMENT NOTIFICATION", task.getAssignedTo(), "New task created has been assigned to you", tasksAssignmentTopicArn);
        return task;
    }

    public Task assignTask(TaskUpdateAssignedToRequest request) {
        Task task = taskRepository.updateAssignedTo(request);
        sqsService.sendToSQS(task, "TASK RE-ASSIGNMENT NOTIFICATION", task.getAssignedTo(), "Task has been re-assigned to you", tasksAssignmentTopicArn);
        return task;
    }

    public Task updateTaskStatus(TaskUpdateStatusRequest request) {
        Task task = taskRepository.updateTaskStatus(request);
        if (request.status().equals(TaskStatus.open.toString())) {
            sqsService.sendToSQS(task, "TASK RE-OPEN NOTIFICATION", task.getAssignedTo(), "Task has been re-opened, ensure you complete it", reopenedTasksTopicArn);
        } else if (request.status().equals(TaskStatus.completed.toString())) {
            sqsService.sendToSQS(task, "TASK COMPLETED NOTIFICATION", task.getCreatedBy(), "Task has been successfully completed", taskCompleteTopicArn);
        }
        return task;
    }

    public TaskResponse getAllTasks() {
        return taskRepository.findAllTasks();
    }

    public TaskResponse getTasksForUser(String userEmail) {
        return taskRepository.findAllTasksByAssignedTo(userEmail);
    }

    public Task getTaskById(String taskId) {
        return taskRepository.findByTaskId(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
    }

    public Task addUserComment(UserCommentRequest request) {
        return taskRepository.setComment(request);
    }

    public Task updateTask(TaskRequest taskRequest, String taskId) {
        Task taskToUpdate = getTaskById(taskId);
        taskToUpdate.setName(taskRequest.name());
        taskToUpdate.setDescription(taskRequest.description());
        taskToUpdate.setDeadline(taskRequest.deadline().format(LocalDateTimeConverter.FORMATTER));

        if(!taskToUpdate.getAssignedTo().equalsIgnoreCase(taskRequest.assignedTo())) {
            taskToUpdate.setAssignedTo(taskRequest.assignedTo());
            sqsService.sendToSQS(taskToUpdate, "TASK RE-ASSIGNMENT NOTIFICATION", taskToUpdate.getAssignedTo(), "Task has been re-assigned to you", tasksAssignmentTopicArn);
        }

        taskRepository.saveTask(taskToUpdate);

        return taskToUpdate;
    }


}
