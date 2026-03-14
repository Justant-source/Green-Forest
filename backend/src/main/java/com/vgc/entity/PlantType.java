package com.vgc.entity;

public enum PlantType {
    TABLE_PALM("테이블야자"),
    SPATHIPHYLLUM("스파티필름"),
    HONG_KONG_PALM("무늬홍콩야자"),
    ORANGE_JASMINE("오렌지자스민");

    private final String label;

    PlantType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
