package org.example.service;

import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;

public interface UserManagementService {
    MessageResponse createUser(UserRequest userRequest);
}
