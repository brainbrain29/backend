package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeDTO {
    private Integer noticeId;
    private String content;
    private String senderName;
    private LocalDateTime createdTime;
    private Integer status;
}
