package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.MessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

//@RestController
//@RequestMapping("/sns")
//@RequiredArgsConstructor
public class SnsSubscriptionController {

//    @Value("${app.sns.topics.tasks-assignment-arn}")
//    private String tasksAssignmentTopicArn;
//
//    @Value("${app.sns.topics.tasks-deadline-arn}")
//    private String tasksDeadlineTopicArn;
//
//    @Value("${app.sns.topics.closed-tasks-arn}")
//    private String closedTasksTopicArn;
//
//    @Value("${app.sns.topics.reopened-tasks-arn}")
//    private String reopenedTasksTopicArn;
//
//    private final SnsClient snsClient;
//
//    @PostMapping(value = "/subscribe", produces = "application/json")
//    public ResponseEntity<MessageResponse> subscribeToAllTopics(@RequestParam String email) {
//        try {
//            subscribeUser(email, tasksAssignmentTopicArn);
//            subscribeUser(email, tasksDeadlineTopicArn);
//            subscribeUser(email, closedTasksTopicArn);
//            subscribeUser(email, reopenedTasksTopicArn);
//            return ResponseEntity.ok(MessageResponse.builder().message("Subscription successful").build());
//        } catch (Exception e) {
//            return new ResponseEntity<>(MessageResponse.builder().message("Error: " + e.getMessage()).build(), HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private void subscribeUser(String email, String topicArn) {
//        SubscribeRequest request = SubscribeRequest.builder()
//                .topicArn(topicArn)
//                .protocol("email")
//                .endpoint(email)
//                .build();
//        snsClient.subscribe(request);
//    }
}
