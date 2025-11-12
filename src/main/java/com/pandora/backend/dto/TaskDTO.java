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
    private String taskStatus;
    private String taskPriority;
    private Integer assigneeId;
    private Integer senderId;
    private String taskType;
    private Integer milestoneId;
    private Integer projectId;

    public TaskDTO() {
    }

    public TaskDTO(Integer taskId, String title, String content, LocalDateTime startTime,
            LocalDateTime endTime, String taskStatus, String taskPriority,
            Integer assigneeId, Integer senderId, String taskType,
            Integer milestoneId, Integer projectId) {
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
        this.milestoneId = milestoneId;
        this.projectId = projectId;
    }
}
