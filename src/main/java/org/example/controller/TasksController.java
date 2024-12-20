package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.service.TaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TasksController {
    private final TaskService taskService;

    @PostMapping(produces = "application/json")
    public void createTask(@RequestBody Task task) {
        taskService.createTask(task);
    }
}
