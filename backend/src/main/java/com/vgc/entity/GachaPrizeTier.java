package com.vgc.entity;

import java.math.BigDecimal;

public enum GachaPrizeTier {
    COMMON("일반", new BigDecimal("1.00")),
    RARE("희귀", new BigDecimal("1.10")),
    EPIC("영웅", new BigDecimal("1.20")),
    LEGENDARY("전설", new BigDecimal("1.30"));

    private final String label;
    private final BigDecimal defaultEvMultiplier;

    GachaPrizeTier(String label, BigDecimal defaultEvMultiplier) {
        this.label = label;
        this.defaultEvMultiplier = defaultEvMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getDefaultEvMultiplier() {
        return defaultEvMultiplier;
    }
}
