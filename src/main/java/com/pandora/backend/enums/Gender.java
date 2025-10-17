package com.pandora.backend.enums;

public enum Gender {
    MALE(1, "男性"),
    FEMALE(0, "女性"),
    UNKNOWN(2, "暂时不知道");

    private final int code;
    private final String desc;

    Gender(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Gender fromCode(int code) {
        for (Gender g : Gender.values()) {
            if (g.code == code)
                return g;
        }
        return UNKNOWN;
    }

    public static Gender fromDesc(String desc) {
        for (Gender g : Gender.values()) {
            if (g.desc.equals(desc))
                return g;
        }
        return UNKNOWN;
    }
}
