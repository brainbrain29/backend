package com.pandora.backend.enums;

// 通知类型
public enum NoticeType {
    NEW_TASK(1, "新任务派发"),
    TASK_UPDATE(2, "任务状态更新"),
    COMPANY_MATTER(3, "公司重要事项发布"),
    IMPORTANT_TASK(4, "公司重要任务发布"),
    CUSTOM(5, "自定义");

    private final int code;
    private final String desc;

    NoticeType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static NoticeType fromCode(int code) {
        for (NoticeType n : NoticeType.values()) {
            if (n.code == code)
                return n;
        }
        return CUSTOM;
    }

    public static NoticeType fromDesc(String desc) {
        for (NoticeType n : NoticeType.values()) {
            if (n.desc.equals(desc))
                return n;
        }
        return CUSTOM;
    }
}
