package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskDTO {
    private Integer taskId;
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Byte taskStatus;
    private Byte taskPriority;
    private Integer assigneeId;
    private Integer senderId;
    private Byte taskType;
    private Byte createdByWho;
    private Integer milestoneId;

    public TaskDTO() {
    }

    public TaskDTO(Integer taskId, String title, String content, LocalDateTime startTime, 
                   LocalDateTime endTime, Byte taskStatus, Byte taskPriority, 
                   Integer assigneeId, Integer senderId, Byte taskType, 
                   Byte createdByWho, Integer milestoneId) {
        this.taskId = taskId;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.assigneeId = assigneeId;
        this.senderId = senderId;
        this.taskType = taskType;
        this.createdByWho = createdByWho;
        this.milestoneId = milestoneId;
    }
}

