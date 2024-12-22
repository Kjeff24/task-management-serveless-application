package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

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

        // Check and attach subscription filter for user email
        checkAndAttachSubscriptionFilter(topicArn, userEmail, context);

        // Check and attach subscription filter for admin email
        checkAndAttachSubscriptionFilter(topicArn, adminEmail, context);

        String message = String.format(
                "Task ID: %s\nAssigned User: %s\nDeadline: %s\nStatus: Expired",
                taskId, userEmail, taskDeadline
        );

        sendSNSNotification(topicArn, userEmail, message, context);
        sendSNSNotification(topicArn, adminEmail, message, context);

        context.getLogger().log("Notification sent for task ID: " + taskId);
        return "Notification sent";
    }

    private void checkAndAttachSubscriptionFilter(String topicArn, String email, Context context) {
        ListSubscriptionsByTopicResponse subscriptionsResponse = snsClient.listSubscriptionsByTopic(
                ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build()
        );

        for (Subscription subscription : subscriptionsResponse.subscriptions()) {
            if (subscription.endpoint().equals(email)) {
                GetSubscriptionAttributesResponse attributesResponse = snsClient.getSubscriptionAttributes(
                        GetSubscriptionAttributesRequest.builder()
                                .subscriptionArn(subscription.subscriptionArn())
                                .build()
                );

                String filterPolicy = attributesResponse.attributes().get("FilterPolicy");

                if (filterPolicy == null || filterPolicy.isEmpty()) {
                    context.getLogger().log("No filter policy found. Attaching a new filter policy for " + email);

                    Map<String, String> newFilterPolicy = Map.of(
                            "email", "[\"" + email + "\"]"
                    );

                    snsClient.setSubscriptionAttributes(
                            SetSubscriptionAttributesRequest.builder()
                                    .subscriptionArn(subscription.subscriptionArn())
                                    .attributeName("FilterPolicy")
                                    .attributeValue(newFilterPolicy.toString())
                                    .build()
                    );

                    context.getLogger().log("Filter policy attached for " + email);
                }
                return; // Exit the loop once the email is found
            }
        }

        context.getLogger().log("No subscription found for email: " + email);
    }

    private void sendSNSNotification(String topicArn, String email, String message, Context context) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .subject("Task Deadline Expired Notification")
                .messageAttributes(Map.of(
                        "email", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(email)
                                .build()
                ))
                .build();

        snsClient.publish(publishRequest);
        context.getLogger().log("Notification sent to " + email);
    }
}
