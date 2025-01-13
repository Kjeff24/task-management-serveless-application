package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class EventBridgeSchedulerLambda implements RequestHandler<Object, Void> {

    private final DynamoDbClient dynamoDbClient;
    private final SqsClient sqsClient;
    private final String tasksTable;
    private final String sqsQueueUrl;
    private final String taskDeadlineTopicArn;

    public EventBridgeSchedulerLambda() {
        dynamoDbClient = DynamoDbClient.create();
        sqsClient = SqsClient.create();
        tasksTable = System.getenv("TASKS_TABLE_NAME");
        sqsQueueUrl = System.getenv("TASKS_QUEUE_URL");
        taskDeadlineTopicArn = System.getenv("TASKS_DEADLINE_TOPIC_ARN");
    }

    @Override
    public Void handleRequest(Object input, Context context) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String currentTime = LocalDateTime.now().format(formatter);

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tasksTable)
                .indexName("StatusAndDeadlineIndex")
                .keyConditionExpression("status = :status")
                .filterExpression("deadline <= :currentTime")
                .expressionAttributeValues(Map.of(
                        ":status", AttributeValue.builder().s("open").build(),
                        ":currentTime", AttributeValue.builder().s(currentTime).build()
                ))
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        for (Map<String, AttributeValue> item : queryResponse.items()) {
            String taskId = item.get("taskId").s();
            String taskDescription = item.get("description").s();
            String taskName = item.get("name").s();
            String deadline = item.get("deadline").s();
            String assignedTo = item.get("assignedTo").s();
            String createdBy = item.get("createdBy").s();
            String status = item.get("status").s();
            int hasSentDeadlineNotification = Integer.parseInt(item.get("hasSentDeadlineNotification").n());
            int hasSentReminderNotification = Integer.parseInt(item.get("hasSentReminderNotification").n());

            LocalDateTime taskDeadline = LocalDateTime.parse(deadline, DateTimeFormatter.ISO_DATE_TIME);

            long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, taskDeadline);
            Map<String, MessageAttributeValue> attributes = new HashMap<>();
            if (minutesUntilDeadline <= 60 && minutesUntilDeadline > 0 && hasSentReminderNotification == 0 && status.equalsIgnoreCase("open")) {
                String taskDetails = "Message: " + "This is a task deadline reminder" +
                        "\nTask ID: " + taskId +
                        "\nTask Description: " + taskDescription +
                        "\nTask Name: " + taskName +
                        "\nTask Deadline: " + deadline +
                        "\nTask Status: " + status;
                attributes.put("sendTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());
                attributes.put("snsTopicArn", MessageAttributeValue.builder().dataType("String").stringValue(taskDeadlineTopicArn).build());
                attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue("TASK DEADLINE REMINDER").build());
                attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("publishToSNS").build());
                sendToSQS(attributes, taskDetails);

                updateDynamoDB(taskId, "hasSentReminderNotification");
                context.getLogger().log("Sent reminder notification for task: " + taskId);

            } else if (minutesUntilDeadline <= 0 && hasSentDeadlineNotification == 0 && status.equalsIgnoreCase("open")) {
                attributes.put("taskId", MessageAttributeValue.builder().dataType("String").stringValue(taskId).build());
                attributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());
                attributes.put("createdBy", MessageAttributeValue.builder().dataType("String").stringValue(createdBy).build());
                attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("taskDeadline").build());
                sendToSQS(attributes, null);

                updateDynamoDB(taskId, "hasSentDeadlineNotification");
                context.getLogger().log("Sent deadline notification for task: " + taskId + " and updated status to expired");
            }
        }

        return null;
    }

    private void sendToSQS(Map<String, MessageAttributeValue> attributes, String taskDetails) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(sqsQueueUrl)
                .messageBody(taskDetails)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }

    private void updateDynamoDB(String taskId, String attribute) {
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(tasksTable)
                .key(Map.of("taskId", AttributeValue.builder().s(taskId).build()))
                .updateExpression("SET " + attribute + " = :value")
                .expressionAttributeValues(Map.of(":value", AttributeValue.builder().n(String.valueOf(1)).build()))
                .build();

        dynamoDbClient.updateItem(updateRequest);
    }
}
