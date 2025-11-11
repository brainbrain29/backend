package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ImportantMatterDTO {
    private Integer matterId;
    private String title;
    private String content;
    private Integer departmentId; // 部门ID
    private String departmentName; // 发布部门名称
    private LocalDateTime publishTime; // 发布时间
    
    // 以下字段为前端模板所需
    private LocalDateTime deadline; // 截止日期
    private String assigneeName; // 负责人姓名
    private Integer assigneeId; // 负责人ID
    private Byte matterStatus; // 事项状态 0:待处理 1:进行中 2:已完成
    private Byte matterPriority; // 事项优先级 0:低 1:中 2:高
    private Byte serialNum; // 序号 1-10
    private Integer visibleRange; // 可见范围 0:全员 1:管理层 2:部门内
}
