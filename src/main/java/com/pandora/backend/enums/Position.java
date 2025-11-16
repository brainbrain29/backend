package com.pandora.backend.enums;

/**
 * 职位枚举类
 * 统一管理职位代码和名称的映射关系
 */
public enum Position {
    CEO(0, "CEO"),
    DEPARTMENT_MANAGER(1, "部门经理"),
    TEAM_LEADER(2, "团队长"),
    EMPLOYEE(3, "员工");

    private final byte code;
    private final String description;

    Position(int code, String description) {
        this.code = (byte) code;
        this.description = description;
    }

    public byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据职位代码获取职位名称
     */
    public static String getDescriptionByCode(Byte code) {
        if (code == null) {
            return "未知";
        }

        for (Position position : values()) {
            if (position.getCode() == code) {
                return position.getDescription();
            }
        }
        return "未知";
    }

    /**
     * 根据职位代码获取枚举
     */
    public static Position getByCode(Byte code) {
        if (code == null) {
            return null;
        }

        for (Position position : values()) {
            if (position.getCode() == code) {
                return position;
            }
        }
        return null;
    }

    /**
     * 根据职位代码获取枚举（别名方法）
     */
    public static Position fromCode(Byte code) {
        return getByCode(code);
    }
}