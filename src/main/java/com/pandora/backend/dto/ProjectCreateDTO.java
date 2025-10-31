package com.pandora.backend.dto;

import java.time.LocalDateTime;

public class ProjectCreateDTO { // TODO: DTO for CEO project creation
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Byte projectStatus;
    private Byte projectPriority;
    private Byte projectType;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Byte getProjectStatus() { return projectStatus; }
    public void setProjectStatus(Byte projectStatus) { this.projectStatus = projectStatus; }
    public Byte getProjectPriority() { return projectPriority; }
    public void setProjectPriority(Byte projectPriority) { this.projectPriority = projectPriority; }
    public Byte getProjectType() { return projectType; }
    public void setProjectType(Byte projectType) { this.projectType = projectType; }
}
