package org.example.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String taskId;
    private String name;
    private String description;
    private String userComment;
    private String assignedTo;
    private String status;
    private String createdBy;
    private String deadline;
    private String completedAt;
    private int hasSentDeadlineNotification;
    private int hasSentReminderNotification;

    @DynamoDbPartitionKey
    public String getTaskId() {
        return taskId;
    }

    @DynamoDbAttribute(value = "name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute(value = "description")
    public String getDescription() {
        return description;
    }

    @DynamoDbAttribute(value = "userComment")
    public String getUserComment() {
        return userComment;
    }

    @DynamoDbAttribute(value = "assignedTo")
    @DynamoDbSecondaryPartitionKey(indexNames = "AssignedToIndex")
    public String getAssignedTo() {
        return assignedTo;
    }

    @DynamoDbAttribute(value = "status")
    @DynamoDbSecondaryPartitionKey(indexNames = "StatusIndex")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute(value = "createdBy")
    @DynamoDbSecondaryPartitionKey(indexNames = "CreatedByIndex")
    public String getCreatedBy() {
        return createdBy;
    }

    @DynamoDbAttribute(value = "deadline")
    @DynamoDbSecondaryPartitionKey(indexNames = "DeadlineIndex")
    public String getDeadline() {
        return deadline;
    }

    @DynamoDbAttribute(value = "completedAt")
    public String getCompletedAt() {
        return completedAt;
    }

    @DynamoDbAttribute(value = "hasSentDeadlineNotification")
    public int getHasSentDeadlineNotification() {
        return hasSentDeadlineNotification;
    }

    @DynamoDbAttribute(value = "hasSentReminderNotification")
    public int getHasSentReminderNotification() {
        return hasSentReminderNotification;
    }

}
