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

    // 负责人信息
    private Integer assigneeId;
    private String assigneeName;

    // 创建者信息
    private Integer senderId;
    private String senderName;

    private String taskType;

    // 里程碑信息
    private Integer milestoneId;
    private String milestoneName;

    public TaskDTO() {
    }

    public TaskDTO(Integer taskId, String title, String content, LocalDateTime startTime,
            LocalDateTime endTime, String taskStatus, String taskPriority,
            Integer assigneeId, String assigneeName, Integer senderId, String senderName,
            String taskType, Integer milestoneId, String milestoneName) {
        this.taskId = taskId;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.senderId = senderId;
        this.senderName = senderName;
        this.taskType = taskType;
        this.milestoneId = milestoneId;
        this.milestoneName = milestoneName;
    }
}
