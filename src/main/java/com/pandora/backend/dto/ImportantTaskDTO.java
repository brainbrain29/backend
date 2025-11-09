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
    private String taskStatus; // 返回中文描述，如"已完成"
    private String taskPriority; // 返回中文描述，如"高"
    private Byte serialNum; // 序号 1-10
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Constructors
    public ImportantTaskDTO() {
    }

    public ImportantTaskDTO(Integer taskId, Integer employeeId, String employeeName,
            String taskContent, LocalDateTime deadline, String taskStatus,
            String taskPriority, Byte serialNum) {
        this.taskId = taskId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.taskContent = taskContent;
        this.deadline = deadline;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.serialNum = serialNum;
    }
}
