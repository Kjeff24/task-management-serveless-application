package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.Map;

public class SNSSubscriptionLambda implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {
    private final SnsClient snsClient;

    private final String tasksAssignmentTopicArn;
    private final String tasksDeadlineTopicArn;
    private final String closedTasksTopicArn;
    private final String reopenedTasksTopicArn;


    public SNSSubscriptionLambda() {
        tasksAssignmentTopicArn = System.getenv("TASKS_ASSIGNMENT_TOPIC_ARN");
        tasksDeadlineTopicArn = System.getenv("TASKS_DEADLINE_TOPIC_ARN");
        closedTasksTopicArn = System.getenv("CLOSED_TASKS_TOPIC_ARN");
        reopenedTasksTopicArn = System.getenv("REOPENED_TASKS_TOPIC_ARN");
        snsClient = SnsClient.create();
    }

    @Override
    public CognitoUserPoolPostConfirmationEvent handleRequest(CognitoUserPoolPostConfirmationEvent event, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");

        subscribeUser(email, tasksAssignmentTopicArn, "Task Assignment Notification");
        subscribeUser(email, tasksDeadlineTopicArn, "Task Deadline Notification");
        subscribeUser(email, closedTasksTopicArn, "Closed Task Notification");
        subscribeUser(email, reopenedTasksTopicArn, "Reopened Task Notification");

        return event;
    }

    private void subscribeUser(String email, String topicArn, String message) {
        ListSubscriptionsByTopicRequest listRequest = ListSubscriptionsByTopicRequest.builder()
                .topicArn(topicArn)
                .build();
        ListSubscriptionsByTopicResponse listResponse = snsClient.listSubscriptionsByTopic(listRequest);

        boolean isSubscribed = false;
        boolean isPending = false;

        for (Subscription subscription : listResponse.subscriptions()) {
            if (subscription.endpoint().equals(email)) {
                if (subscription.subscriptionArn().equals("PendingConfirmation")) {
                    isPending = true;
                } else {
                    isSubscribed = true;
                }
                break;
            }
        }

        if (isSubscribed) {
            System.out.println("User is already subscribed to the topic: " + topicArn);
        } else if (isPending) {
            sendNotificationWithoutSubscription(email, message);
            System.out.println("Subscription is pending for user: " + email);
        } else {
            SubscribeRequest request = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("email")
                    .endpoint(email)
                    .build();
            snsClient.subscribe(request);
        }
    }

    private void sendNotificationWithoutSubscription(String email, String message) {
        PublishRequest publishRequest = PublishRequest.builder()
                .message(message)
                .subject("Confirm Subscription")
                .topicArn(email)
                .messageAttributes(Map.of(
                        "email", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(email)
                                .build()))
                .build();

        snsClient.publish(publishRequest);
    }

}