package com.pandora.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

// in dto package
@Data
public class LogSummaryDTO {
    private Integer logId;
    private LocalDateTime createdTime;
    private String contentSummary; // 日志内容摘要
}
