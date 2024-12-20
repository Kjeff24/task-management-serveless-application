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
    @Value("${app.sns.topics.tasks-assignment-arn}")
    private String tasksAssignmentTopicArn;
    @Value("${app.sqs.task.url}")
    private String taskQueueUrl;

    public void sendToSQS(Task task) {
        String taskDetails = "Task ID: " + task.getTaskId() +
                "\nTask Description: " + task.getDescription() +
                "\nTask Deadline: " + task.getDeadline() +
                "\nTask Status: " + task.getStatus();

        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("assignedTo", MessageAttributeValue.builder().dataType("String").stringValue(task.getAssignedTo()).build());
        attributes.put("taskId", MessageAttributeValue.builder().dataType("String").stringValue(task.getTaskId()).build());
        attributes.put("snsTopicArn", MessageAttributeValue.builder().dataType("String").stringValue(tasksAssignmentTopicArn).build());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(taskQueueUrl)
                .messageBody(taskDetails)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }

}
