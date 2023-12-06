package com.chronomon.analysis.trajectory.road;

public enum DirectionEnum {
    DUAL_DIRECT(1, "双向"),
    FORWARD_DIRECT(2, "正向"),
    BACKWARD_DIRECT(3, "逆向"),
    UN_KNOWN(4, "未知");

    public final int code;
    public final String desc;

    DirectionEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DirectionEnum getByCode(int code) {
        switch (code) {
            case 1:
                return DUAL_DIRECT;
            case 2:
                return FORWARD_DIRECT;
            case 3:
                return BACKWARD_DIRECT;
            case 4:
                return UN_KNOWN;
            default:
                throw new IllegalArgumentException("无法识别的道路方向:" + code);
        }
    }
}