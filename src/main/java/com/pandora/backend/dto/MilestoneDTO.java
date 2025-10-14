package com.pandora.backend.dto;

public class MilestoneDTO {
    private String title;
    private Byte milestoneNo;

    public MilestoneDTO(String title, Byte milestoneNo) {
        this.title = title;
        this.milestoneNo = milestoneNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Byte getMilestoneNo() {
        return milestoneNo;
    }

    public void setMilestoneNo(Byte milestoneNo) {
        this.milestoneNo = milestoneNo;
    }
}
