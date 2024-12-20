package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.HashMap;
import java.util.Map;


public class SNSNotificationLambda implements RequestHandler<SQSEvent, Void> {
    private final SnsClient snsClient;

    public SNSNotificationLambda() {
        snsClient = SnsClient.create();
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            String taskId = message.getMessageAttributes().get("taskId").getStringValue();
            String assignedTo = message.getMessageAttributes().get("assignedTo").getStringValue();
            String snsTopicArn = message.getMessageAttributes().get("snsTopicArn").getStringValue();
            String messageBody = message.getBody();

            sendSNSNotification(assignedTo, taskId, snsTopicArn, messageBody);
        }
        return null;
    }

    private void sendSNSNotification(String assignedTo, String taskId, String snsTopicArn, String message) {

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(assignedTo).build());

        PublishRequest publishRequest = PublishRequest.builder()
                .subject("Important Notification")
                .topicArn(snsTopicArn)
                .message(message)
                .messageAttributes(messageAttributes)
                .build();

        snsClient.publish(publishRequest);
    }
}