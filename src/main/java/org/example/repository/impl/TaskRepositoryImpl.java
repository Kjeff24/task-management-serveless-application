package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.enums.TaskStatus;
import org.example.exception.BadRequestException;
import org.example.exception.NotFoundException;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.example.util.LocalDateTimeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    public Optional<Task> findByTaskId(String taskId) {
        Key key = Key.builder().partitionValue(taskId).build();
        return Optional.ofNullable(getTable().getItem(r -> r.key(key)));
    }

    public Task setComment(UserCommentRequest request) {
        Task task = findByTaskId(request.taskId()).orElseThrow(() -> new NotFoundException("Task not found"));
        task.setUserComment(request.comment());
        saveTask(task);
        return task;
    }

    public TaskResponse findAllTasksByAssignedTo(String assignedTo) {
        DynamoDbIndex<Task> index = getTable().index("AssignedToIndex");
        List<Task> allTaskByAssignedTo = index.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(assignedTo))))
                .stream()
                .map(Page::items)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return toTaskResponse(allTaskByAssignedTo);
    }

    public TaskResponse findAllTasks() {
        List<Task> allTasks = getTable().scan().items().stream()
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return toTaskResponse(allTasks);
    }

    public void deleteTask(String taskId) {
        Key key = Key.builder().partitionValue(taskId).build();
        getTable().deleteItem(r -> r.key(key));
    }

    public Task updateTaskStatus(TaskUpdateStatusRequest request) {
        Task task = findByTaskId(request.taskId()).orElseThrow(() -> new NotFoundException("Task not found"));

        if (TaskStatus.open.toString().equals(request.status()) &&
                LocalDateTime.parse(task.getDeadline(), LocalDateTimeConverter.FORMATTER).isBefore(LocalDateTime.now()) &&
                request.newDeadline() == null) {
            throw new BadRequestException("Deadline must be provided when setting the task status to open and the current deadline has passed.");
        }

        if (TaskStatus.completed.toString().equals(request.status())) {
            task.setCompletedAt(LocalDateTime.now().format(LocalDateTimeConverter.FORMATTER));
        } else if (TaskStatus.open.toString().equals(request.status())) {
            task.setCompletedAt("");

        }
        if (request.newDeadline() != null) {
            task.setDeadline(request.newDeadline().format(LocalDateTimeConverter.FORMATTER));
        }

        task.setStatus(request.status());
        saveTask(task);
        return task;
    }

    public Task updateAssignedTo(TaskUpdateAssignedToRequest request) {
        Task task = findByTaskId(request.taskId()).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setAssignedTo(request.assignedTo());
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
        Task task = findByTaskId(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setUserComment(comment);
        saveTask(task);
        return task;
    }

    public void updateHasSentDeadlineNotification(String taskId, boolean hasSentNotification) {
        Task task = findByTaskId(taskId).orElseThrow(
                () -> new NotFoundException("Task not found")
        );
        task.setHasSentDeadlineNotification(hasSentNotification ? 1 : 0);
        saveTask(task);
    }

    private TaskResponse toTaskResponse(List<Task> tasks) {
        List<Task> completedTasks = tasks.stream()
                .filter(task -> TaskStatus.completed.toString().equalsIgnoreCase(task.getStatus()))
                .toList();

        List<Task> openTasks = tasks.stream()
                .filter(task -> TaskStatus.open.toString().equalsIgnoreCase(task.getStatus()))
                .toList();

        return TaskResponse.builder()
                .open(openTasks)
                .completed(completedTasks)
                .build();
    }
}
