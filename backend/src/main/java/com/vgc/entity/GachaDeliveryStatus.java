package com.vgc.entity;

public enum GachaDeliveryStatus {
    NONE("해당없음"),
    PENDING("전달대기"),
    DELIVERED("전달완료");

    private final String label;

    GachaDeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
