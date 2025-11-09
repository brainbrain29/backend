package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

//TODO: 用于项目分配,需要返回团队名字吗
@Getter
@Setter
public class AssignDTO {
    private Integer assigneeId; // 任务负责人ID (用于任务分配)
    private Integer teamId; // 团队ID (用于项目分配)
}