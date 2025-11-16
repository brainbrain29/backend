package com.pandora.backend.dto;

import com.pandora.backend.enums.Position;
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
    private Byte position; // 保留原有的 Byte 类型，用于兼容
    private String positionName; // 新增：职位名称，如 "CEO"、"部门经理"
    private Integer orgId;
    private String orgName;
}
