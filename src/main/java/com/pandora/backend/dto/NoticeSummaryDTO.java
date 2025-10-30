package com.pandora.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

// in dto package
@Data
public class NoticeSummaryDTO {
    private Integer id;
    private String title;
    private String tag; // 标签
    private String summary;
    private LocalDateTime publishTime;
    private boolean unread;
}