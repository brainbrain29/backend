package com.pandora.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityLogDTO {
    private String activityType;      // 活动类型：ADD_MATTER, COMPLETE_TASK, ADD_EMPLOYEE, UPDATE_TASK
    private String description;       // 活动描述
    private String employeeName;      // 操作人姓名
    private LocalDateTime timestamp;  // 时间戳
    private String relativeTime;      // 相对时间（如"10分钟前"）
}
