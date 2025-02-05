package org.example.service;

import org.example.dto.UserRequest;
import org.example.dto.UserResponse;

import java.util.List;

public interface UserManagementService {
    UserResponse createUser(UserRequest userRequest);

    List<UserResponse> getAllTeamMembers();
    List<UserResponse> getAllUsers();
}
