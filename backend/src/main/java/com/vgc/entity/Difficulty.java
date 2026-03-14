package com.vgc.entity;

import java.math.BigDecimal;

public enum Difficulty {
    EASY("쉬움", new BigDecimal("1.00")),
    NORMAL("보통", new BigDecimal("1.10")),
    HARD("어려움", new BigDecimal("1.20"));

    private final String label;
    private final BigDecimal multiplier;

    Difficulty(String label, BigDecimal multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }

    public String getLabel() { return label; }
    public BigDecimal getMultiplier() { return multiplier; }
}
