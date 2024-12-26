package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.service.UserManagementService;
import org.example.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final SnsClient snsClient;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    @Value("${app.aws.cognito.user.pool.id}")
    private String userPoolId;
    @Value("${app.aws.sns.topics.tasks.assignment.arn}")
    private String tasksAssignmentTopicArn;
    @Value("${app.aws.sns.topics.tasks.reopened.arn}")
    private String tasksReopenedTopicArn;
    @Value("${app.aws.sns.topics.tasks.closed.arn}")
    private String tasksClosedTopicArn;
    @Value("${app.aws.sns.topics.tasks.deadline.arn}")
    private String tasksDeadlineTopicArn;


    public MessageResponse createUser(UserRequest userRequest) {
        List<AttributeType> userAttributes = new ArrayList<>();
        userAttributes.add(AttributeType.builder().name("email").value(userRequest.email()).build());
        userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(userRequest.email())
                .userAttributes(userAttributes)
                .temporaryPassword(PasswordGenerator.generatePassword(9))
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .build();
        cognitoIdentityProviderClient.adminCreateUser(createUserRequest);

        subscribeUserToSNSTopic(tasksAssignmentTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksClosedTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksReopenedTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksDeadlineTopicArn, userRequest.email());

        return MessageResponse.builder().message("User creation successful").build();
    }


    public List<UserType> getAllUsers() {
        List<UserType> allUsers = new ArrayList<>();
        String paginationToken = null;

        do {
            ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .paginationToken(paginationToken)
                    .build();

            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(listUsersRequest);

            allUsers.addAll(response.users());

            paginationToken = response.paginationToken();

        } while (paginationToken != null);

        return allUsers;
    }

    private void subscribeUserToSNSTopic(String topicArn, String email) {
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .build();

        snsClient.subscribe(request);
    }
}
