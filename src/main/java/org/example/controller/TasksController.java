package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Task;
import org.example.service.TaskService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TasksController {
    private final TaskService taskService;


    @GetMapping
    public String hello() {
        return "Hello World";
    }

    @PostMapping(produces = "application/json")
    public void createTask(@RequestBody Task task) {
        taskService.createTask(task);
    }
}
