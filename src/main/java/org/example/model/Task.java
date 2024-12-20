package org.example.model;

import lombok.Data;
import org.example.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class Task {
    private String taskId;
    private String title;
    private String description;
    private String assignedTo;
    private TaskStatus status;
    private String createdBy;
    private LocalDate creationDate;
    private LocalTime deadline;
    private String priority;
}
