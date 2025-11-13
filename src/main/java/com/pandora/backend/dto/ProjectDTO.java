package com.pandora.backend.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectDTO {
    private Integer projectId;
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String projectStatus; // 返回中文描述给前端，如"已完成"
    
    // 创建者信息
    private Integer senderId;
    private String senderName;
    
    // 团队信息
    private Integer teamId;
    private String teamName;
}
