package com.pandora.backend.enums;

// 通知状态
public enum NoticeStatus {
    NOT_VIEWED(0, "未查看"),
    VIEWED(1, "已查看"),
    NOT_RECEIVED(2, "未接收");

    private final int code;
    private final String desc;

    NoticeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static NoticeStatus fromCode(int code) {
        for (NoticeStatus n : NoticeStatus.values()) {
            if (n.code == code)
                return n;
        }
        return NOT_VIEWED;
    }

    public static NoticeStatus fromDesc(String desc) {
        for (NoticeStatus n : NoticeStatus.values()) {
            if (n.desc.equals(desc))
                return n;
        }
        return NOT_VIEWED;
    }
}
