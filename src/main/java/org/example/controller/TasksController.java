package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
import org.example.dto.TaskUpdateAssignedToRequest;
import org.example.dto.TaskUpdateStatusRequest;
import org.example.exception.NotAuthorizedException;
import org.example.model.Task;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/hello")
    public String hello(@AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminEmail = jwt.getClaimAsString("email");
        return "groups: " + groups + ", id: " + adminEmail;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest task, @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminEmail = jwt.getClaimAsString("email");

        if (groups != null && groups.contains(adminGroup)) {
            return new ResponseEntity<>(taskService.createTask(task, adminEmail), HttpStatus.CREATED);
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @PutMapping("/assign")
    public ResponseEntity<Task> assignTask(@Valid @RequestBody TaskUpdateAssignedToRequest request, @AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(taskService.assignTask(request));
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @PutMapping("/status")
    public ResponseEntity<Task> updateTaskStatus(@Valid @RequestBody TaskUpdateStatusRequest request, @AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(taskService.updateTaskStatus(request));
        } else if ("completed".equals(request.status())) {
            return ResponseEntity.ok(taskService.updateTaskStatus(request));
        }else {
            throw new NotAuthorizedException("You are unauthorized to change task status to open.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(@AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(taskService.getAllTasks());
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<Task>> getTasksForUser(@PathVariable("userEmail") String userEmail, @AuthenticationPrincipal Jwt jwt) {
        System.out.println("userEmail: " + userEmail);
        return ResponseEntity.ok(taskService.getTasksForUser(userEmail));
    }

    @GetMapping("/user")
    public ResponseEntity<List<Task>> findAssignedTasksByUser(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(taskService.getTasksForUser(userEmail));
    }

    @GetMapping("/find-task")
    public ResponseEntity<Task> getTaskById(@RequestParam("taskId") String taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }
}
