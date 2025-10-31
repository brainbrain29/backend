package com.pandora.backend.dto;

public class MilestoneCreateDTO { // TODO: DTO for CEO milestone creation
    private String title;
    private String content;
    private Integer projectId;
    private Byte milestoneNo;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Byte getMilestoneNo() { return milestoneNo; }
    public void setMilestoneNo(Byte milestoneNo) { this.milestoneNo = milestoneNo; }
}
