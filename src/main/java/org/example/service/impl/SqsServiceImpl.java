package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.service.SqsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SqsServiceImpl implements SqsService {
    private final SqsClient sqsClient;
    @Value("${app.aws.sqs.task.url}")
    private String taskQueueUrl;

    public void sendToSQS(Task task, String subject, String message, String topicArn) {
        String taskDetails = "Message: " + message +
                "Task ID: " + task.getTaskId() +
                "\nTask Description: " + task.getDescription() +
                "\nTask Name: " + task.getName() +
                "\nTask Deadline: " + task.getDeadline() +
                "\nTask Status: " + task.getStatus();

        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(task.getAssignedTo()).build());
        attributes.put("createdBy", MessageAttributeValue.builder().dataType("String").stringValue(task.getCreatedBy()).build());
        attributes.put("snsTopicArn", MessageAttributeValue.builder().dataType("String").stringValue(topicArn).build());
        attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue(subject).build());
        attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("publishToSNS").build());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(taskQueueUrl)
                .messageBody(taskDetails)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }

}
