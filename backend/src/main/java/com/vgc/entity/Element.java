package com.vgc.entity;

public enum Element {
    EARTH("땅"),
    WATER("물"),
    WIND("바람"),
    FIRE("불");

    private final String label;

    Element(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
