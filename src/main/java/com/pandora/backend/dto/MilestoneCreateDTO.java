package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilestoneCreateDTO {
    private String title;
    private String content;
    private Integer projectId;
    private Byte milestoneNo;

}
