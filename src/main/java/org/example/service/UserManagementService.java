package org.example.service;

import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;

public interface UserManagementService {
    MessageResponse createUser(UserRequest userRequest);

    List<UserType> getAllUsers();
}
