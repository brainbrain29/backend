package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignableEmployeeDTO {
    private Integer employeeId;
    private String employeeName;
    private String email;
    private Byte position;
    private String positionName;
    private Integer orgId;
    private String orgName;
}
