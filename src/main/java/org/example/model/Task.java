package org.example.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@DynamoDBTable(tableName = "${app.aws.dynamodb.task.table}")
@Data
public class Task {
    @DynamoDBHashKey(attributeName = "taskId")
    private String taskId;

    @DynamoDBAttribute(attributeName = "title")
    private String title;

    @DynamoDBAttribute(attributeName = "description")
    private String description;

    @DynamoDBAttribute(attributeName = "assignedTo")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "AssignedToIndex")
    private String assignedTo;

    @DynamoDBAttribute(attributeName = "status")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "StatusIndex")
    private String status;

    @DynamoDBAttribute(attributeName = "createdBy")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "CreatedByIndex")
    private String createdBy;

    @DynamoDBAttribute(attributeName = "deadline")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "DeadlineIndex")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime deadline;

    @DynamoDBAttribute(attributeName = "hasSentDeadlineNotification")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "HasSentDeadlineNotificationIndex")
    private boolean hasSentDeadlineNotification;

}
