package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentDTO {
    private Integer orgId;
    private String orgName;

    // Constructors
    public DepartmentDTO() {}

    public DepartmentDTO(Integer orgId, String orgName) {
        this.orgId = orgId;
        this.orgName = orgName;
    }
}


