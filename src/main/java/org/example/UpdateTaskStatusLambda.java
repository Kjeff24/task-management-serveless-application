package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;

public class UpdateTaskStatusLambda implements RequestHandler<Map<String, Object>, String> {

    private final DynamoDbClient dynamoDbClient;
    private final String taskTableName;

    public UpdateTaskStatusLambda() {
        dynamoDbClient = DynamoDbClient.create();
        taskTableName = System.getenv("TASKS_TABLE_NAME");
    }

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Received event: " + event);

        String taskId = (String) event.get("TaskId");
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is missing.");
        }

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(taskTableName)
                .key(Map.of("taskId", AttributeValue.builder().s(taskId).build()))
                .updateExpression("SET #status = :status")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(":status", AttributeValue.builder().s("expired").build()))
                .returnValues("UPDATED_NEW")
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(updateRequest);

        context.getLogger().log("Updated task status to 'expired' for task ID: " + taskId);
        return response.attributes().toString();
    }
}
