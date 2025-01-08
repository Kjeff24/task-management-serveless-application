package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.dto.UserResponse;
import org.example.mapper.UserMapper;
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
    private final UserMapper userMapper;
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
    @Value("${app.aws.cognito.admin.group}")
    private String adminGroup;


    public UserResponse createUser(UserRequest userRequest) {
        List<AttributeType> userAttributes = new ArrayList<>();
        userAttributes.add(AttributeType.builder().name("email").value(userRequest.email()).build());
        userAttributes.add(AttributeType.builder().name("name").value(userRequest.fullName()).build());
        userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(userRequest.email())
                .userAttributes(userAttributes)
                .temporaryPassword(PasswordGenerator.generatePassword(9))
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .build();

        AdminCreateUserResponse createUserResponse = cognitoIdentityProviderClient.adminCreateUser(createUserRequest);

        UserResponse userResponse = userMapper.toUserResponse(createUserResponse.user());

        subscribeUserToSNSTopic(tasksAssignmentTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksClosedTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksReopenedTopicArn, userRequest.email());
        subscribeUserToSNSTopic(tasksDeadlineTopicArn, userRequest.email());

        return userResponse;
    }

    public List<UserResponse> getAllUsers() {
        List<UserResponse> allUserResponses = new ArrayList<>();
        String paginationToken = null;

        do {
            ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .paginationToken(paginationToken)
                    .build();

            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(listUsersRequest);

            for (UserType user : response.users()) {
                boolean isInApiAdminsGroup = cognitoIdentityProviderClient.adminListGroupsForUser(
                                AdminListGroupsForUserRequest.builder()
                                        .username(user.username())
                                        .userPoolId(userPoolId)
                                        .build()
                        ).groups().stream()
                        .anyMatch(group -> adminGroup.equals(group.groupName()));

                if (!isInApiAdminsGroup) {
                    UserResponse userResponse = userMapper.toUserResponse(user);
                    allUserResponses.add(userResponse);
                }
            }

            paginationToken = response.paginationToken();

        } while (paginationToken != null);

        return allUserResponses;
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
