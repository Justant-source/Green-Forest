package com.vgc.entity;

public enum SnackSsgCategory {
    SNACK("스낵/과자", "6000223618"),
    COOKIE("쿠키/비스킷/크래커", "6000223619"),
    SAND("샌드/스틱과자", "6000223620"),
    PIE("파이/케익", "6000223621"),
    BABY("유아과자/시리얼", "6000223622"),
    TRADITIONAL("떡/한과/전통과자", "6000223623"),
    ANJU("소시지/원물/안주스낵", "6000223624"),
    CANDY("사탕/카라멜/껌", "6000223625"),
    JELLY("젤리/푸딩", "6000223626"),
    CHOCOLATE("초콜릿/초코바", "6000223627"),
    CEREAL("시리얼", "6000223628"),
    ICE_CREAM("아이스크림/빙수", "6000223629");

    private final String label;
    private final String dispCtgId;

    SnackSsgCategory(String label, String dispCtgId) {
        this.label = label;
        this.dispCtgId = dispCtgId;
    }

    public String getLabel() {
        return label;
    }

    public String getDispCtgId() {
        return dispCtgId;
    }

    public String themeUrl() {
        return "https://m.ssg.com/disp/theme/category.ssg?dispCtgId=" + dispCtgId;
    }
}
