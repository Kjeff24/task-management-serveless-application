package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
import org.example.exception.NotAuthorizedException;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TasksController {
    private final TaskService taskService;
    @Value("${app.aws.cognito.admin.group}")
    private String adminGroup;

    @GetMapping
    public String hello( @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminId = jwt.getSubject();
        return "groups: " + groups + ", id: " + adminId;
    }

    @PostMapping(produces = "application/json")
    public void createTask(@RequestBody TaskRequest task, @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminId = jwt.getSubject();

        if (groups != null && groups.contains(adminGroup)) {
            // User is in the admin group
            taskService.createTask(task, adminId);
        } else {
            // Throw an exception or return an error response
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }
}
