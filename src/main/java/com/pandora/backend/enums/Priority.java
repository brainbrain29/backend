package com.pandora.backend.enums;

// 项目/任务/事项优先级
public enum Priority {
    HIGH(1, "重要"),
    NORMAL(2, "一般"),
    LOW(3, "日常");

    private final int code;
    private final String desc;

    Priority(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Priority fromCode(int code) {
        for (Priority p : Priority.values()) {
            if (p.code == code)
                return p;
        }
        return NORMAL;
    }

    public static Priority fromDesc(String desc) {
        for (Priority p : Priority.values()) {
            if (p.desc.equals(desc))
                return p;
        }
        return NORMAL;
    }
}
