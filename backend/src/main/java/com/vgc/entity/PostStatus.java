package com.vgc.entity;

public enum PostStatus {
    REGISTERED("등록"),
    ING("진행중"),
    COMPLETE("완료");

    private final String label;

    PostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
