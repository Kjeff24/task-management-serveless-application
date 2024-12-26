package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;

public class SQSEventLambda implements RequestHandler<SQSEvent, Void> {
    private final String stateMachineArn;

    private final SfnClient stepFunctionsClient;
    private final SnsClient snsClient;

    public SQSEventLambda() {
        stepFunctionsClient = SfnClient.create();
        snsClient = SnsClient.create();
        stateMachineArn = System.getenv("STATE_MACHINE_ARN");
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            String workflowType = message.getMessageAttributes().get("workflowType").getStringValue();

            if ("taskDeadline".equals(workflowType)) {
                handleTaskDeadline(message, context);
            } else if ("publishToSNS".equals(workflowType)) {
                handlePublishToSNS(message, context);
            } else {
                context.getLogger().log("Unknown workflowType: " + workflowType);
            }
        }
        return null;
    }

    private void handleTaskDeadline(SQSEvent.SQSMessage message, Context context) {
        context.getLogger().log("Processing TaskDeadline event: " + message);

        String taskId = message.getMessageAttributes().get("taskId").getStringValue();
        String assignedUser = message.getMessageAttributes().get("assignedTo").getStringValue();
        String admin = message.getMessageAttributes().get("createdBy").getStringValue();
        String taskDeadline = message.getMessageAttributes().get("deadline").getStringValue();

        if (taskId == null || assignedUser == null || admin == null || taskDeadline == null) {
            throw new IllegalArgumentException("Missing required task details in the event.");
        }

        String input = String.format(
                "{" +
                        "\"workflowType\": \"taskDeadline\", " +
                        "\"taskId\": \"%s\", " +
                        "\"assignedTo\": \"%s\", " +
                        "\"createdBy\": \"%s\", " +
                        "\"deadline\": \"%s\"" +
                        "}",
                taskId, assignedUser, admin, taskDeadline
        );

        StartExecutionRequest request = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .input(input)
                .build();

        StartExecutionResponse response = stepFunctionsClient.startExecution(request);

        context.getLogger().log("Started Step Function Execution: " + response.executionArn());
    }

    private void handlePublishToSNS(SQSEvent.SQSMessage message, Context context) {
        context.getLogger().log("Processing PublishToSNS event: " + message);

        String assignedTo = message.getMessageAttributes().get("assignedTo").getStringValue();
        String createdBy = message.getMessageAttributes().get("createdBy").getStringValue();
        String snsTopicArn = message.getMessageAttributes().get("snsTopicArn").getStringValue();
        String subject = message.getMessageAttributes().get("subject").getStringValue();
        String messageBody = message.getBody();

        sendSNSNotification(assignedTo, createdBy, snsTopicArn, messageBody, subject);
    }

    private void sendSNSNotification(String assignedTo, String createdBy, String snsTopicArn, String message, String subject) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        if (assignedTo != null && !assignedTo.isEmpty()) {
            messageAttributes.put("assignedTo", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(assignedTo)
                    .build());
        }

        if (createdBy != null && !createdBy.isEmpty()) {
            messageAttributes.put("createdBy", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(createdBy)
                    .build());
        }

        addSubscriptionFilter(snsTopicArn, assignedTo, createdBy);

        PublishRequest publishRequest = PublishRequest.builder()
                .subject(subject)
                .topicArn(snsTopicArn)
                .message(message)
                .messageAttributes(messageAttributes)
                .build();

        snsClient.publish(publishRequest);
    }

    private void addSubscriptionFilter(String snsTopicArn, String assignedTo, String createdBy) {
        ListSubscriptionsByTopicResponse subscriptionsResponse = snsClient.listSubscriptionsByTopic(
                ListSubscriptionsByTopicRequest.builder()
                        .topicArn(snsTopicArn)
                        .build()
        );

        for (Subscription subscription : subscriptionsResponse.subscriptions()) {
            String subscriptionArn = subscription.subscriptionArn();

            if ("PendingConfirmation".equals(subscriptionArn) || "Deleted".equals(subscriptionArn)) {
                continue;
            }

            GetSubscriptionAttributesResponse attributesResponse = snsClient.getSubscriptionAttributes(
                    GetSubscriptionAttributesRequest.builder()
                            .subscriptionArn(subscriptionArn)
                            .build()
            );

            Map<String, String> attributes = attributesResponse.attributes();
            String endpoint = attributes.get("Endpoint");

            if (assignedTo.equals(endpoint)) {
                String updatedFilterPolicy = String.format(String.format("{\"assignedTo\": [\"%s\"]}", assignedTo));

                snsClient.setSubscriptionAttributes(
                        SetSubscriptionAttributesRequest.builder()
                                .subscriptionArn(subscriptionArn)
                                .attributeName("FilterPolicy")
                                .attributeValue(updatedFilterPolicy)
                                .build()
                );
            }

            if (createdBy.equals(endpoint)) {
                String updatedFilterPolicy = String.format(String.format("{\"createdBy\": [\"%s\"]}", createdBy));


                snsClient.setSubscriptionAttributes(
                        SetSubscriptionAttributesRequest.builder()
                                .subscriptionArn(subscriptionArn)
                                .attributeName("FilterPolicy")
                                .attributeValue(updatedFilterPolicy)
                                .build()
                );
            }

            if (!assignedTo.equals(endpoint) && !createdBy.equals(endpoint)) {
                String defaultFilterPolicy = "{\"assignedTo\": [\"none\"]}";
                snsClient.setSubscriptionAttributes(
                        SetSubscriptionAttributesRequest.builder()
                                .subscriptionArn(subscriptionArn)
                                .attributeName("FilterPolicy")
                                .attributeValue(defaultFilterPolicy)
                                .build()
                );
            }

        }
    }

}

