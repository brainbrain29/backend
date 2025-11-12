package com.pandora.backend.dto;

import com.pandora.backend.enums.PositionEnum;

/**
 * 用于更新员工职位的DTO
 */
public class UpdatePositionDTO {

    /**
     * 新的职位
     * Spring Boot 会自动将 JSON 字符串（如 "DEPARTMENT_MANAGER"）转换为 PositionEnum 枚举类型
     */
    private PositionEnum newPosition;

    /**
     * 目标实体的ID，可选
     * - 当新职位是部门经理时，此ID为部门ID (Department ID)
     * - 当新职位是团队长时，此ID为团队ID (Team ID)
     */
    private Integer targetId;

    // --- Getters and Setters ---
    public PositionEnum getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(PositionEnum newPosition) {
        this.newPosition = newPosition;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }
}