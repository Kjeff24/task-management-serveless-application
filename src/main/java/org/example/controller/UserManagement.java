package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.dto.UserResponse;
import org.example.service.UserManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;

@RestController
@RequestMapping("/user-management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('apiAdmins')")
public class UserManagement {
    private final UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<MessageResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(userManagementService.createUser(userRequest));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

}
