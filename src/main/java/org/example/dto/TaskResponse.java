package org.example.dto;

import lombok.Builder;
import lombok.Data;
import org.example.model.Task;

import java.util.List;

@Builder
public class TaskResponse {
    List<Task> open;
    List<Task> completed;
}
