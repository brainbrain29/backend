package com.pandora.backend.enums;

public enum Emoji {
    HAPPY(1, "开心"),
    PRESSURE(2, "压力"),
    PEACE(3, "平静"),
    FATIGUE(4, "疲惫"),
    ANGRY(5, "生气");

    private final int code;
    private final String desc;

    Emoji(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Emoji fromCode(int code) {
        for (Emoji g : Emoji.values()) {
            if (g.code == code)
                return g;
        }
        return PEACE;
    }

    public static Emoji fromDesc(String desc) {
        for (Emoji g : Emoji.values()) {
            if (g.desc.equals(desc))
                return g;
        }
        return PEACE;
    }
}
