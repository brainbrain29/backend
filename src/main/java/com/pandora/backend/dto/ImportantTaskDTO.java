package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ImportantTaskDTO {
    private Integer taskId;
    private Integer employeeId;
    private String employeeName;
    private String taskContent;
    private LocalDateTime deadline;
    private Byte taskStatus;
    private Byte taskPriority;
    private Byte serialNum;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Constructors
    public ImportantTaskDTO() {}

    public ImportantTaskDTO(Integer taskId, Integer employeeId, String employeeName,
                                 String taskContent, LocalDateTime deadline, Byte taskStatus,
                                 Byte taskPriority, Byte serialNum, LocalDateTime createdTime,
                                 LocalDateTime updatedTime) {
        this.taskId = taskId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.taskContent = taskContent;
        this.deadline = deadline;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.serialNum = serialNum;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }
}
