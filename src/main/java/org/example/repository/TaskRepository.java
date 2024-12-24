package org.example.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskRepository {
    private final DynamoDBMapper dynamoDBMapper;

    public void saveTask(Task task) {
        dynamoDBMapper.save(task);

    }

    public Task getTaskById(String taskId) {
        return dynamoDBMapper.load(Task.class, taskId);
    }

    public List<Task> getTasksByAssignedTo(String assignedTo) {
        // Use DynamoDBMapper's query functionality to fetch tasks based on assignedTo
        return new ArrayList<>();
    }

    public void deleteTask(String taskId) {
        dynamoDBMapper.delete(getTaskById(taskId));
    }
}
