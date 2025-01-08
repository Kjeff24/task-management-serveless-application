package org.example.service;

import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.dto.UserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;

public interface UserManagementService {
    UserResponse createUser(UserRequest userRequest);

    List<UserResponse> getAllUsers();
}
