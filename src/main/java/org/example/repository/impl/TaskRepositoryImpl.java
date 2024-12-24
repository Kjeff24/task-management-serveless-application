package org.example.repository.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {
    private final DynamoDBMapper dynamoDBMapper;

    public void saveTask(Task task) {
        dynamoDBMapper.save(task);

    }

    public Task getTaskById(String taskId) {
        return dynamoDBMapper.load(Task.class, taskId);
    }

    public List<Task> getTasksByAssignedTo(String assignedTo) {
        Task task = new Task();
        task.setAssignedTo(assignedTo);

        DynamoDBQueryExpression<Task> queryExpression = new DynamoDBQueryExpression<Task>()
                .withHashKeyValues(task)
                .withConsistentRead(false);
        return dynamoDBMapper.query(Task.class, queryExpression);
    }

    public List<Task> getAllTasks() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return dynamoDBMapper.scan(Task.class, scanExpression);
    }

    public void deleteTask(String taskId) {
        dynamoDBMapper.delete(getTaskById(taskId));
    }

    public Task updateTaskStatus(String taskId, String status) {
        Task task = getTaskById(taskId);
        if (task != null) {
            task.setStatus(status);
            saveTask(task);
        }

        return task;
    }

    public Task updateAssignedTo(String taskId, String userId) {
        Task task = getTaskById(taskId);
        if (task != null) {
            task.setAssignedTo(userId);
            saveTask(task);
        }

        return task;
    }

    public List<Task> getTasksByStatus(String status) {
        Task task = new Task();
        task.setStatus(status);

        DynamoDBQueryExpression<Task> queryExpression = new DynamoDBQueryExpression<Task>()
                .withHashKeyValues(task)
                .withConsistentRead(false);

        return dynamoDBMapper.query(Task.class, queryExpression);
    }

    public List<Task> getTasksByCreatedBy(String createdBy) {
        Task task = new Task();
        task.setCreatedBy(createdBy);

        DynamoDBQueryExpression<Task> queryExpression = new DynamoDBQueryExpression<Task>()
                .withHashKeyValues(task)
                .withConsistentRead(false);

        return dynamoDBMapper.query(Task.class, queryExpression);
    }

    public List<Task> getTasksNearDeadline() {

        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        Task task = new Task();
        task.setDeadline(oneHourFromNow);

        DynamoDBQueryExpression<Task> queryExpression = new DynamoDBQueryExpression<Task>()
                .withHashKeyValues(task)
                .withConsistentRead(false);

        return dynamoDBMapper.query(Task.class, queryExpression);
    }

    public Task updateTaskComment(String taskId, String comment) {
        Task task = getTaskById(taskId);
        if (task != null) {
            task.setUserComment(comment);
            saveTask(task);
        }
        return task;
    }

    public void updateHasSentDeadlineNotification(String taskId, boolean hasSentNotification) {
        Task task = getTaskById(taskId);
        if (task != null) {
            task.setHasSentDeadlineNotification(hasSentNotification ? 1 : 0);
            saveTask(task);
        }
    }
}
