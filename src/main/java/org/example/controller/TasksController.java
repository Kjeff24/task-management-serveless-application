package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.enums.TaskStatus;
import org.example.exception.NotAuthorizedException;
import org.example.model.Task;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> getAllTasks() {

        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest task, @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        return new ResponseEntity<>(taskService.createTask(task, adminEmail), HttpStatus.CREATED);
    }


    @PutMapping("/update/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Task> updateTask(@Valid @RequestBody TaskRequest task, @PathVariable("taskId") String taskId) {
        return ResponseEntity.ok(taskService.updateTask(task, taskId));
    }

    @PutMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Task> assignTask(@Valid @RequestBody TaskUpdateAssignedToRequest request) {
        return ResponseEntity.ok(taskService.assignTask(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PutMapping("/status")
    public ResponseEntity<Task> updateTaskStatus(@Valid @RequestBody TaskUpdateStatusRequest request, @AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        boolean isAdmin = groups != null && groups.contains(adminGroup);
        boolean isCompleted = TaskStatus.completed.toString().equals(request.status());
        if (isAdmin || isCompleted) {
            return ResponseEntity.ok(taskService.updateTaskStatus(request));
        } else {
            throw new NotAuthorizedException("You are unauthorized to change task status to open.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userEmail}")
    public ResponseEntity<TaskResponse> getTasksForUser(@PathVariable("userEmail") String userEmail) {
        return ResponseEntity.ok(taskService.getTasksForUser(userEmail));
    }


    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public ResponseEntity<TaskResponse> findAssignedTasksByUser(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(taskService.getTasksForUser(userEmail));
    }

    @GetMapping("/find-task")
    public ResponseEntity<Task> getTaskById(@RequestParam("taskId") String taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/comment")
    public ResponseEntity<Task> addUserComment(@Valid @RequestBody UserCommentRequest request) {
        return ResponseEntity.ok(taskService.addUserComment(request));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/assign-to")
    public ResponseEntity<Task> assignToUser(@Valid @RequestBody AssignToRequest request) {
        return ResponseEntity.ok(taskService.changeAssignedToUser(request));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable("taskId") String taskId) {
        taskService.deleteTask(taskId);
    }
}
