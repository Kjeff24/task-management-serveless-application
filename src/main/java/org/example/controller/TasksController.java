package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.TaskRequest;
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
    public String hello( @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminId = jwt.getSubject();
        return "groups: " + groups + ", id: " + adminId;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskRequest task, @AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminId = jwt.getSubject();

        if (groups != null && groups.contains(adminGroup)) {
            return new ResponseEntity<>(taskService.createTask(task, adminId), HttpStatus.CREATED);
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @PutMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<Task> assignTask(@PathVariable("taskId") String taskId, @PathVariable("userId") String userId, @AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(taskService.assignTask(taskId, userId));
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @PutMapping("/{taskId}/status/{status}")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable("taskId") String taskId, @PathVariable("status") String status, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status));
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(@AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        String adminId = jwt.getSubject();

        if (groups != null && groups.contains(adminGroup)) {
            return ResponseEntity.ok(taskService.getAllTasks());
        } else {
            throw new NotAuthorizedException("User does not have permission to create tasks.");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getTasksForUser(@PathVariable("userId") String userId, @AuthenticationPrincipal Jwt jwt) {
        System.out.println("userId: " + userId);
        return ResponseEntity.ok(taskService.getTasksForUser(userId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<Task>> findAssignedTasksByUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(taskService.getTasksForUser(userId));
    }

    @GetMapping("/find-task")
    public ResponseEntity<Task> getTaskById(@RequestParam("taskId") String taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }
}
