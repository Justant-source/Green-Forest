package com.vgc.entity.event;

public enum EventType {
    PHOTO_BINGO("사진빙고");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
