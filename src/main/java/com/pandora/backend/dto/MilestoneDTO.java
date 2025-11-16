package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilestoneDTO {
    private Integer milestoneId;
    private String title;

    public MilestoneDTO(Integer milestoneId, String title) {
        this.milestoneId = milestoneId;
        this.title = title;
    }

    public Integer getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Integer milestoneId) {
        this.milestoneId = milestoneId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
