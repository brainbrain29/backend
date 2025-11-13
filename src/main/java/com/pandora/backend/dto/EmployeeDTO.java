package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeDTO {
    private Integer employeeId;
    private String employeeName;
    private String gender; // 前端看到文字 "男性"/"女性"
    private String phone;
    private String email;
    private Byte position;
    private Integer orgId;
    private String orgName;

}
