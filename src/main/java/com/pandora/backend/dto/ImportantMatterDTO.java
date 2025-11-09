package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ImportantMatterDTO {
    private Integer eventId;
    private String title;
    private String content;
    private Integer departmentId; // 部门ID
    private String departmentName; // 发布部门名称
    private LocalDateTime publishTime; // 发布时间
}
