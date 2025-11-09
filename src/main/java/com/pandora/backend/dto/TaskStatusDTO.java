package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusDTO {
    private Integer taskId;
    private String taskStatus; // 前端传递中文描述，如"已完成"
}
