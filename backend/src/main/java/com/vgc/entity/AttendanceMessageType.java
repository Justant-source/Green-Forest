package com.vgc.entity;

public enum AttendanceMessageType {
    CUSTOM("직접입력"),
    SUGGESTED("추천선택");

    private final String label;

    AttendanceMessageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
