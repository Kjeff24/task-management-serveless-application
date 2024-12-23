package org.example.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.example.model.Task;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepository {
    private DynamoDBMapper dynamoDBMapper;

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
        Task task = new Task();
        task.setTaskId(taskId);
        dynamoDBMapper.delete(task);
    }
}
