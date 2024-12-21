package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

public class SNSSubscriptionLambda implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SNSSubscriptionLambda.class);

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

        sendSubscriptionRequest(email, tasksAssignmentTopicArn);
        sendSubscriptionRequest(email, tasksDeadlineTopicArn);
        sendSubscriptionRequest(email, closedTasksTopicArn);
        sendSubscriptionRequest(email, reopenedTasksTopicArn);

        return event;
    }

    private void sendSubscriptionRequest(String email, String topicArn) {
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .build();

        snsClient.subscribe(request);
        logger.info("Sent subscription request for email {} to topic {}", email, topicArn);
    }
}
