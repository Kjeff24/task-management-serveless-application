package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.UserRequest;
import org.example.dto.UserResponse;
import org.example.enums.Role;
import org.example.mapper.UserMapper;
import org.example.service.UserManagementService;
import org.example.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final SfnClient stepFunctionsClient;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final UserMapper userMapper;
    @Value("${app.aws.cognito.user.pool.id}")
    private String userPoolId;
    @Value("${app.aws.sfn.arn}")
    private String stateMachineArn;
    @Value("${app.aws.cognito.admin.group}")
    private String adminGroup;
    @Value("${app.aws.cognito.team.group}")
    private String teamGroup;



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

        AdminAddUserToGroupRequest addUserToGroupRequest = null;
        if(userRequest.role().equalsIgnoreCase(Role.USER.toString())) {
            addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userRequest.email())
                    .groupName(teamGroup)
                    .build();
        } else  if(userRequest.role().equalsIgnoreCase(Role.ADMIN.toString())) {
            addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userRequest.email())
                    .groupName(adminGroup)
                    .build();
        }

        cognitoIdentityProviderClient.adminAddUserToGroup(addUserToGroupRequest);

        UserResponse userResponse = userMapper.toUserResponse(createUserResponse.user());

        startStepFunction(userRequest.email(), userRequest.role());

        return userResponse;
    }

    public List<UserResponse> getAllTeamMembers() {
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
                UserResponse userResponse = userMapper.toUserResponse(user);
                allUserResponses.add(userResponse);
            }

            paginationToken = response.paginationToken();

        } while (paginationToken != null);

        return allUserResponses;
    }


    private void startStepFunction(String userEmail, String role) {
        String input = String.format("{\"workflowType\":\"onboarding\",\"userEmail\":\"%s\",\"role\":\"%s\"}", userEmail, role.toUpperCase());
        StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .input(input)
                .build();

        stepFunctionsClient.startExecution(executionRequest);
    }
}
