package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
//    private final SqsService sqsService;

    @Value("${app.aws.dynamodb.task.table}")
    private String taskTableName;

    public void createTask(Task task) {
        taskRepository.saveTask(task);
//        sqsService.sendToSQS(task);
    }


}
