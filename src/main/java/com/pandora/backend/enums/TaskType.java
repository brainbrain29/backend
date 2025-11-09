package com.pandora.backend.enums;

public enum TaskType {
    YES(1, "需要审核"),
    NO(0, "无需审核");

    private final int code;
    private final String desc;

    TaskType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TaskType fromCode(int code) {
        for (TaskType g : TaskType.values()) {
            if (g.code == code)
                return g;
        }
        return YES;
    }

    public static TaskType fromDesc(String desc) {
        for (TaskType g : TaskType.values()) {
            if (g.desc.equals(desc))
                return g;
        }
        return YES;
    }
}
