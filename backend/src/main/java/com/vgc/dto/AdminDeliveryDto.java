package com.vgc.dto;

import com.vgc.entity.GachaDraw;

import java.time.LocalDateTime;

public class AdminDeliveryDto {
    private Long id;
    private String userNickname;
    private String prizeName;
    private int prizeCashValue;
    private String deliveryStatus;
    private String deliveryMemo;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    public static AdminDeliveryDto from(GachaDraw d) {
        AdminDeliveryDto dto = new AdminDeliveryDto();
        dto.id = d.getId();
        dto.userNickname = d.getUser() != null ? d.getUser().getNickname() : null;
        dto.prizeName = d.getPrizeName();
        dto.prizeCashValue = d.getPrizeCashValue();
        dto.deliveryStatus = d.getDeliveryStatus().name();
        dto.deliveryMemo = d.getDeliveryMemo();
        dto.createdAt = d.getCreatedAt();
        dto.deliveredAt = d.getDeliveredAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUserNickname() { return userNickname; }
    public String getPrizeName() { return prizeName; }
    public int getPrizeCashValue() { return prizeCashValue; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}
