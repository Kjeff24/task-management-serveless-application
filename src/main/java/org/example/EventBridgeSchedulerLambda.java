package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime oneHourFromNow = now.plusHours(1);

        checkTasks(now, oneHourFromNow, context, true);
        checkTasks(now, null, context, false);

        return null;
    }

    private void checkTasks(LocalDateTime now, LocalDateTime endTime, Context context, boolean isReminderCheck) {
        QueryRequest queryRequest = buildQueryRequest(now, endTime);
        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        for (Map<String, AttributeValue> item : queryResponse.items()) {
            handleTask(item, now, context, isReminderCheck);
        }
    }

    private QueryRequest buildQueryRequest(LocalDateTime now, LocalDateTime endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", AttributeValue.builder().s("open").build());
        values.put(":currentTime", AttributeValue.builder().s(now.format(formatter)).build());

        String keyCondition;
        if (endTime != null) {
            values.put(":endTime", AttributeValue.builder().s(endTime.format(formatter)).build());
            keyCondition = "#status = :status AND #deadline BETWEEN :currentTime AND :endTime";
        } else {
            keyCondition = "#status = :status AND #deadline <= :currentTime";
        }

        return QueryRequest.builder()
                .tableName(tasksTable)
                .indexName("StatusAndDeadlineIndex")
                .keyConditionExpression(keyCondition)
                .expressionAttributeNames(Map.of("#status", "status", "#deadline", "deadline"))
                .expressionAttributeValues(values)
                .build();
    }

    private void handleTask(Map<String, AttributeValue> item, LocalDateTime now, Context context, boolean isReminderCheck) {
        String taskId = item.get("taskId").s();
        String deadline = item.get("deadline").s();
        LocalDateTime taskDeadline = LocalDateTime.parse(deadline, DateTimeFormatter.ISO_DATE_TIME);

        String attributeToUpdate = isReminderCheck ? "hasSentReminderNotification" : "hasSentDeadlineNotification";
        int notificationSent = Integer.parseInt(item.get(attributeToUpdate).n());

        if (shouldSendNotification(now, taskDeadline, isReminderCheck) && notificationSent == 0) {
            Map<String, MessageAttributeValue> attributes = prepareAttributes(item, isReminderCheck);
            sendToSQS(attributes, buildTaskDetails(item, isReminderCheck));
            updateDynamoDB(taskId, attributeToUpdate);
            context.getLogger().log("Sent notification for task: " + taskId);
        }
    }

    private boolean shouldSendNotification(LocalDateTime now, LocalDateTime taskDeadline, boolean isReminderCheck) {
        long minutesUntilDeadline = now.until(taskDeadline, ChronoUnit.MINUTES);
        return isReminderCheck ? (minutesUntilDeadline <= 60 && minutesUntilDeadline > 0) : minutesUntilDeadline <= 0;
    }

    private Map<String, MessageAttributeValue> prepareAttributes(Map<String, AttributeValue> item, boolean isReminderCheck) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        String assignedTo = item.get("assignedTo").s();
        attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue(isReminderCheck ? "publishToSNS" : "taskDeadline").build());
        if (isReminderCheck) {
            attributes.put("sendTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());
            attributes.put("snsTopicArn", MessageAttributeValue.builder().dataType("String").stringValue(taskDeadlineTopicArn).build());
            attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue("TASK DEADLINE REMINDER").build());
        } else {
            attributes.put("taskId", MessageAttributeValue.builder().dataType("String").stringValue(item.get("taskId").s()).build());
            attributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());
            attributes.put("createdBy", MessageAttributeValue.builder().dataType("String").stringValue(item.get("createdBy").s()).build());
            attributes.put("deadline", MessageAttributeValue.builder().dataType("String").stringValue(item.get("deadline").s()).build());
        }
        return attributes;
    }

    private String buildTaskDetails(Map<String, AttributeValue> item, boolean isReminderCheck) {
        return "Message: " + (isReminderCheck ? "Deadline Reminder Notification" : "Deadline Notification") +
                "\nTask ID: " + item.get("taskId").s() +
                "\nTask Description: " + item.get("description").s() +
                "\nTask Name: " + item.get("name").s() +
                "\nTask Deadline: " + item.get("deadline").s() +
                "\nTask Status: " + item.get("status").s();
    }

    private void sendToSQS(Map<String, MessageAttributeValue> attributes, String taskDetails) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsQueueUrl)
                .messageBody(taskDetails)
                .messageAttributes(attributes)
                .build());
    }

    private void updateDynamoDB(String taskId, String attribute) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tasksTable)
                .key(Map.of("taskId", AttributeValue.builder().s(taskId).build()))
                .updateExpression("SET " + attribute + " = :value")
                .expressionAttributeValues(Map.of(":value", AttributeValue.builder().n("1").build()))
                .build());
    }
}
