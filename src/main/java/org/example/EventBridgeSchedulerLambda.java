package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class EventBridgeSchedulerLambda implements RequestHandler<Object, Void> {

    private final DynamoDbClient dynamoDbClient;
    private final SqsClient sqsClient;
    private final String tasksTable;
    private final String sqsQueueUrl;

    public  EventBridgeSchedulerLambda() {
        dynamoDbClient = DynamoDbClient.create();
        sqsClient = SqsClient.create();
        tasksTable = System.getenv("TASKS_TABLE_NAME");
        sqsQueueUrl = System.getenv("TASKS_QUEUE_URL");
    }

    @Override
    public Void handleRequest(Object input, Context context) {
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.toString();

        // Query DynamoDB for tasks with deadlines less than or equal to the current time
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tasksTable)
                .filterExpression("deadline <= :currentTime AND #status <> :completed")
                .expressionAttributeValues(Map.of(
                        ":currentTime", AttributeValue.builder().s(currentTime).build(),
                        ":completed", AttributeValue.builder().s("completed").build()
                ))
                .expressionAttributeNames(Map.of("#status", "status"))
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        for (Map<String, AttributeValue> item : scanResponse.items()) {
            String taskId = item.get("taskId").s();
            String taskDescription = item.get("description").s();
            String deadline = item.get("deadline").s();
            String assignedTo = item.get("assignedTo").s();
            String createdBy = item.get("createdBy").s();

            // Prepare task details to send to SQS
            String taskDetails = "Task ID: " + taskId + "\nTask Description: " + taskDescription + "\nTask Deadline: " + deadline;

            Map<String, MessageAttributeValue> attributes = new HashMap<>();
            attributes.put("taskId", MessageAttributeValue.builder().dataType("String").stringValue(taskId).build());
            attributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());
            attributes.put("createdBy", MessageAttributeValue.builder().dataType("String").stringValue(createdBy).build());
            attributes.put("deadline", MessageAttributeValue.builder().dataType("String").stringValue(deadline).build()); // Ensure deadline is included
            attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("taskDeadline").build()); // Ensure deadline is included

            // Send task details to SQS
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(taskDetails)
                    .messageAttributes(attributes)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
            context.getLogger().log("Sent task to SQS: " + taskId);
        }

        return null;
    }
}
