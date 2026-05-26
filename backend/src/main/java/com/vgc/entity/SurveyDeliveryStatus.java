package com.vgc.entity;

public enum SurveyDeliveryStatus {
    PENDING("배송대기"),
    SHIPPED("배송중"),
    DELIVERED("배송완료"),
    CANCELED("취소");

    private final String label;

    SurveyDeliveryStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
