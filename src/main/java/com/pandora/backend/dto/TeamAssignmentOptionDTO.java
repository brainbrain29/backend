package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for team assignment options
 * 用于项目分配时显示可选的团队
 */
@Getter
@Setter
public class TeamAssignmentOptionDTO {
    private Integer teamId;
    private String teamName;
}