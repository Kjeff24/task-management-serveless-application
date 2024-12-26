package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.MessageResponse;
import org.example.dto.UserRequest;
import org.example.exception.NotAuthorizedException;
import org.example.service.UserManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;

@RestController
@RequestMapping("/user-management")
@RequiredArgsConstructor
public class UserManagement {
    private final UserManagementService userManagementService;
    @Value("${app.aws.cognito.admin.group}")
    private String adminGroup;

    @PostMapping
    public ResponseEntity<MessageResponse> createUser(@Valid @RequestBody UserRequest userRequest, @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(userManagementService.createUser(userRequest));
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @GetMapping
    public ResponseEntity<List<UserType>> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(userManagementService.getAllUsers());
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

}
