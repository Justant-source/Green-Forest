package com.vgc.entity;

public enum RequestRejectReason {
    OUT_OF_STOCK("재고부족"),
    POLICY("정책제외"),
    DUPLICATE("중복"),
    OTHER("기타");

    private final String label;

    RequestRejectReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
