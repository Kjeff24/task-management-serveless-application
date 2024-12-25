package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.NotFoundException;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public static final TableSchema<Task> TASK_TABLE_SCHEMA = TableSchema
            .fromBean(Task.class);

    @Value("${app.aws.dynamodb.task.table}")
    private String taskTableName;

    private DynamoDbTable<Task> getTable() {
        return dynamoDbEnhancedClient.table(taskTableName, TASK_TABLE_SCHEMA);
    }


    public void saveTask(Task task) {
        getTable().putItem(task);
    }


    public Optional<Task> getTaskById(String taskId) {
        Key key = Key.builder().partitionValue(taskId).build();
        return Optional.ofNullable(getTable().getItem(r -> r.key(key)));
    }


    public List<Task> getTasksByAssignedTo(String assignedTo) {
        DynamoDbIndex<Task> index = getTable().index("AssignedToIndex");

        return index.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(assignedTo))))
                .stream()
                .map(Page::items)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }




    public List<Task> getAllTasks() {
        return getTable().scan().items().stream().collect(Collectors.toList());
    }


    public void deleteTask(String taskId) {
        Key key = Key.builder().partitionValue(taskId).build();
        getTable().deleteItem(r -> r.key(key));
    }


    public Task updateTaskStatus(String taskId, String status) {
        Task task = getTaskById(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setStatus(status);
        saveTask(task);
        return task;
    }


    public Task updateAssignedTo(String taskId, String userId) {
        Task task = getTaskById(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setAssignedTo(userId);
        saveTask(task);
        return task;
    }

    public List<Task> getTasksByStatus(String status) {
        DynamoDbIndex<Task> index = getTable().index("StatusIndex");

        Expression expression = Expression.builder()
                .expression("status = :status")
                .expressionValues(Map.of(":status", AttributeValue.builder().s(status).build()))
                .build();

        return index.query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(status)))
                        .filterExpression(expression))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }



    public List<Task> getTasksByCreatedBy(String createdBy) {
        DynamoDbIndex<Task> index = getTable().index("CreatedByIndex");

        Expression expression = Expression.builder()
                .expression("createdBy = :createdBy")
                .expressionValues(Map.of(":createdBy", AttributeValue.builder().s(createdBy).build()))
                .build();

        return index.query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(createdBy)))
                        .filterExpression(expression))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public List<Task> getTasksNearDeadline() {
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        DynamoDbIndex<Task> index = getTable().index("DeadlineIndex");

        Expression expression = Expression.builder()
                .expression("deadline <= :deadline")
                .expressionValues(Map.of(":deadline", AttributeValue.builder().s(oneHourFromNow.toString()).build()))
                .build();

        return index.query(r -> r.queryConditional(QueryConditional.sortLessThanOrEqualTo(k -> k.sortValue(oneHourFromNow.toString())))
                        .filterExpression(expression))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }



    public List<Task> getTasksNearDeadlineWholeTable() {
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        Expression expression = Expression.builder()
                .expression("deadline <= :deadline")
                .expressionValues(Map.of(":deadline", AttributeValue.builder().s(oneHourFromNow.toString()).build()))
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();

        return getTable().scan(scanRequest)
                .items()
                .stream()
                .collect(Collectors.toList());
    }



    public Task updateTaskComment(String taskId, String comment) {
        Task task = getTaskById(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setUserComment(comment);
        saveTask(task);
        return task;
    }


    public void updateHasSentDeadlineNotification(String taskId, boolean hasSentNotification) {
        Task task = getTaskById(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setHasSentDeadlineNotification(hasSentNotification ? 1 : 0);
        saveTask(task);
    }
}
