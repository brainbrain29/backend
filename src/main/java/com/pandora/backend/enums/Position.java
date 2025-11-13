package com.pandora.backend.enums;

/**
 * 职位枚举
 */
public enum Position {
    CEO(0, "CEO"),
    DEPARTMENT_MANAGER(1, "部门经理"),
    PROJECT_MANAGER(2, "项目经理"),
    EMPLOYEE(3, "普通员工");

    private final Integer code;
    private final String description;

    Position(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static Position fromCode(Integer code) {
        if (code == null) {
            return EMPLOYEE; // 默认普通员工
        }
        for (Position position : Position.values()) {
            if (position.code.equals(code)) {
                return position;
            }
        }
        return EMPLOYEE; // 默认普通员工
    }

    /**
     * 根据 Byte code 获取枚举
     */
    public static Position fromCode(Byte code) {
        return fromCode(code == null ? null : code.intValue());
    }

    /**
     * 根据描述获取枚举
     */
    public static Position fromDescription(String description) {
        if (description == null) {
            return EMPLOYEE;
        }
        for (Position position : Position.values()) {
            if (position.description.equals(description)) {
                return position;
            }
        }
        return EMPLOYEE;
    }
}
