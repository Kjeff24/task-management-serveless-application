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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    @Value("${app.aws.cognito.user.pool.id}")
    private String userPoolId;

    public MessageResponse createUser(UserRequest userRequest) {
        List<AttributeType> userAttributes = new ArrayList<>();
        userAttributes.add(AttributeType.builder().name("email").value(userRequest.email()).build());
        userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(userRequest.username())
                .userAttributes(userAttributes)
                .temporaryPassword(PasswordGenerator.generatePassword(9))
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .build();
        cognitoIdentityProviderClient.adminCreateUser(createUserRequest);
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
}
