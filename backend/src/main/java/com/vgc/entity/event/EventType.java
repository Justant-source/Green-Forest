package com.vgc.entity.event;

public enum EventType {
    PHOTO_BINGO("사진빙고"),
    PHOTO_EXHIBITION("사진 전시회");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
