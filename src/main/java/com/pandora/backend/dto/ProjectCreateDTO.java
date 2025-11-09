package com.pandora.backend.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectCreateDTO {
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String projectStatus;
    private Integer teamId;
}
