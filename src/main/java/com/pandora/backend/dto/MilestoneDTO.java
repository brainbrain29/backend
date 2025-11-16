package com.pandora.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDTO {
    private Integer milestoneId;
    private String title;
}
