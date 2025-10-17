package com.pandora.backend.enums;

// 项目/任务/事项状态
public enum Status {
    NOT_FINISHED(1, "未完成"),
    COMPLETED(2, "已完成"),
    PENDING_REVIEW(3, "待审核"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String desc;

    Status(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Status fromCode(int code) {
        for (Status s : Status.values()) {
            if (s.code == code)
                return s;
        }
        return NOT_FINISHED; // 默认值
    }

    public static Status fromDesc(String desc) {
        for (Status s : Status.values()) {
            if (s.desc.equals(desc))
                return s;
        }
        return NOT_FINISHED;
    }
}
