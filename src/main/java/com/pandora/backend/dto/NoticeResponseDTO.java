package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeResponseDTO {
    private Integer noticeId;
    private Integer senderId;
    private String senderName;
    private String content;
    private Integer noticeType;
    private LocalDateTime createdTime;
    private Integer status;
}