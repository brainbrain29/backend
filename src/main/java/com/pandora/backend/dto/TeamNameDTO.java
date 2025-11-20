package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 团队名称DTO
 * 用于返回团队的基本信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamNameDTO {
    private Integer teamId;
    private String teamName;
    private Integer departmentId;
    private String departmentName;
}
