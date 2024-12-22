package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.service.SqsService;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final DynamoDbClient dynamoDbClient;
    private final SqsService sqsService;

    @Value("${app.dynamodb.task.table}")
    private String taskTableName;

    public void createTask(Task task) {
        // Insert the task into DynamoDB
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.builder().s(task.getTaskId()).build());
        item.put("assignedTo", AttributeValue.builder().s(task.getAssignedTo()).build());
        item.put("title", AttributeValue.builder().s(task.getTitle()).build());
        item.put("description", AttributeValue.builder().s(task.getDescription()).build());
        item.put("status", AttributeValue.builder().s(String.valueOf(task.getStatus())).build());
        item.put("createdBy", AttributeValue.builder().s(task.getCreatedBy()).build());
        item.put("creationDate", AttributeValue.builder().s(String.valueOf(task.getDeadline())).build());
        item.put("deadline", AttributeValue.builder().s(String.valueOf(task.getDeadline())).build());
        item.put("priority", AttributeValue.builder().s(task.getPriority()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(taskTableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);

        sqsService.sendToSQS(task);
    }


}
