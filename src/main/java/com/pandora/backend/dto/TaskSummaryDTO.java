package com.pandora.backend.dto;

import lombok.Data;

import java.time.LocalDate;

// in dto package
@Data
public class TaskSummaryDTO {
    private Integer taskId;
    private String title;
    private String taskPriority; // 优先级，返回中文描述
    private String taskStatus; // 状态，返回中文描述
    private String assigneeName; // 负责人
    private LocalDate dueDate; // 到期日
}