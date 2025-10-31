package com.pandora.backend.dto;

import lombok.Data;

import java.time.LocalDate;

// in dto package
@Data
public class TaskSummaryDTO {
    private Integer taskId;
    private String title;
    private Byte priority;
    private Byte status;
    private String assigneeName; // 负责人
    private LocalDate dueDate; // 到期日
}