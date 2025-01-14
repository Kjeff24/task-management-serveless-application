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

        String message = String.format(
                "Message: Task has expired\nTask ID: %s\nAssigned User: %s\nDeadline: %s\nStatus: Expired",
                taskId, userEmail, taskDeadline
        );

        sendSNSNotification(topicArn, userEmail, adminEmail, message, context);

        context.getLogger().log("Notification sent for task ID: " + taskId);
        return "Notification sent";
    }


    private void sendSNSNotification(String topicArn, String userEmail, String adminEmail, String message, Context context) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        if (userEmail != null && !userEmail.isEmpty()) {
            messageAttributes.put("userEmail", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(userEmail)
                    .build());
        }

        if (adminEmail != null && !adminEmail.isEmpty()) {
            messageAttributes.put("userEmail", MessageAttributeValue.builder()
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

            String endpoint = snsClient.getSubscriptionAttributes(
                    GetSubscriptionAttributesRequest.builder()
                            .subscriptionArn(subscriptionArn)
                            .build()
            ).attributes().get("Endpoint");

            String filterPolicy;

            if (assignedTo.equals(endpoint)) {
                filterPolicy = String.format("{\"userEmail\": [\"%s\"]}", assignedTo);
            } else if (createdBy.equals(endpoint)) {
                filterPolicy = String.format("{\"userEmail\": [\"%s\"]}", createdBy);
            } else {
                filterPolicy = "{\"userEmail\": [\"none\"]}";
            }

            snsClient.setSubscriptionAttributes(
                    SetSubscriptionAttributesRequest.builder()
                            .subscriptionArn(subscriptionArn)
                            .attributeName("FilterPolicy")
                            .attributeValue(filterPolicy)
                            .build()
            );

        }
    }
}
