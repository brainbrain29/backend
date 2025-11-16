package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set; // 导入Set

@Getter
@Setter
public class LogDTO {
    private Integer logId;
    private String employeeName;
    // 这里假设前端能存储员工id和任务id
    private Integer employeeId;
    private String taskName;
    private Integer taskId;
    private LocalDateTime createdTime;
    private String content;
    private String emoji;
    private Set<AttachmentDTO> attachments;
    private String employeeLocation;
}
