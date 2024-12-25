package org.example.service.impl;

import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.service.UserManagementService;
import org.example.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private CognitoIdentityProviderClient cognitoIdentityProviderClient;
    @Value("${app.aws.cognito.user.pool.id}")
    private String userPoolId;

    public MessageResponse createUser(UserRequest userRequest) {
        List<AttributeType> userAttributes = new ArrayList<>();
        userAttributes.add(AttributeType.builder().name("email").value(userRequest.email()).build());
        userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .userAttributes(userAttributes)
                .temporaryPassword(PasswordGenerator.generatePassword(9))
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .build();
        cognitoIdentityProviderClient.adminCreateUser(createUserRequest);
        return MessageResponse.builder().message("User creation successful").build();
    }
}
