package com.vgc.entity;

public enum JobClass {
    TANKER("탱커", "Guardian"),
    HEALER("힐러", "Healer"),
    BUFFER("버퍼", "Enchanter"),
    DEALER("딜러", "Striker");

    private final String label;
    private final String labelEn;

    JobClass(String label, String labelEn) {
        this.label = label;
        this.labelEn = labelEn;
    }

    public String getLabel() { return label; }
    public String getLabelEn() { return labelEn; }
}
