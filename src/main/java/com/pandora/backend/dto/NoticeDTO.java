package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeDTO {
    private Integer noticeId;
    private String title; // 通知标题(通知类型描述)
    private String content;
    private String senderName;
    private LocalDateTime createdTime;
    private String status; // 通知状态描述: "未查看"/"已查看"/"未接收"
    private Integer relatedId; // 关联ID(任务ID/重要事项ID等),前端根据title判断调用哪个接口
}
