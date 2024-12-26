package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;

public class SendDeadlineNotificationLambda implements RequestHandler<Map<String, Object>, String> {

    private final SnsClient snsClient;

    public SendDeadlineNotificationLambda() {
        this.snsClient = SnsClient.create();
    }

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Received event: " + event);

        String topicArn = (String) event.get("TopicArn");
        String taskId = (String) event.get("TaskId");
        String userEmail = (String) event.get("UserEmail");
        String adminEmail = (String) event.get("AdminEmail");
        String taskDeadline = (String) event.get("Deadline");

        if (topicArn == null || taskId == null || userEmail == null || taskDeadline == null) {
            throw new IllegalArgumentException("Missing required fields in the event.");
        }

        String message = String.format(
                "Task ID: %s\nAssigned User: %s\nDeadline: %s\nStatus: Expired",
                taskId, userEmail, taskDeadline
        );

        sendSNSNotification(topicArn, userEmail, adminEmail, message, context);

        context.getLogger().log("Notification sent for task ID: " + taskId);
        return "Notification sent";
    }


    private void sendSNSNotification(String topicArn, String userEmail, String adminEmail, String message, Context context) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        if (userEmail != null && !userEmail.isEmpty()) {
            messageAttributes.put("assignedTo", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(userEmail)
                    .build());
        }

        if (adminEmail != null && !adminEmail.isEmpty()) {
            messageAttributes.put("createdBy", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(adminEmail)
                    .build());
        }

        addSubscriptionFilter(topicArn, userEmail, adminEmail);

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .subject("Task Deadline Expired Notification")
                .messageAttributes(messageAttributes)
                .build();

        snsClient.publish(publishRequest);
        context.getLogger().log("Notification sent to " + userEmail + " and " + adminEmail);
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
