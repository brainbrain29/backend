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

    // Getter / Setter
    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Byte getPosition() {
        return position;
    }

    public void setPosition(Byte position) {
        this.position = position;
        // 自动设置 positionName
        this.positionName = Position.fromCode(position).getDescription();
    }

    /**
     * 设置 position 并自动更新 positionName
     */
    public void setPositionWithName(Byte position) {
        this.position = position;
        this.positionName = Position.fromCode(position).getDescription();
    }
}
