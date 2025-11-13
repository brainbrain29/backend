package com.pandora.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDTO {
    private String title;
    private Byte milestoneNo;    
}
