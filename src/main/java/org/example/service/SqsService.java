package org.example.service;

import org.example.model.Task;

public interface SqsService {
    void sendToSQS(Task task, String subject, String to, String message, String topicArn);
}
