package com.vgc.dto;

import com.vgc.entity.AttendanceCheckin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminAttendanceDeliveryDto {
    private Long id;
    private LocalDate winDate;
    private Long userId;
    private String userName;
    private String userNickname;
    private String userEmail;
    private LocalDateTime checkinAt;
    private LocalDateTime winnerDrawnAt;
    private String message;
    private String deliveryStatus;
    private String deliveryMemo;
    private LocalDateTime deliveredAt;

    public static AdminAttendanceDeliveryDto from(AttendanceCheckin c) {
        AdminAttendanceDeliveryDto dto = new AdminAttendanceDeliveryDto();
        dto.id = c.getId();
        dto.winDate = c.getCheckinDate();
        if (c.getUser() != null) {
            dto.userId = c.getUser().getId();
            dto.userName = c.getUser().getName();
            dto.userNickname = c.getUser().getNickname();
            dto.userEmail = c.getUser().getEmail();
        }
        dto.checkinAt = c.getCheckinAt();
        dto.winnerDrawnAt = c.getWinnerDrawnAt();
        dto.message = c.getMessage();
        dto.deliveryStatus = c.getDeliveryStatus().name();
        dto.deliveryMemo = c.getDeliveryMemo();
        dto.deliveredAt = c.getDeliveredAt();
        return dto;
    }

    public Long getId() { return id; }
    public LocalDate getWinDate() { return winDate; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserNickname() { return userNickname; }
    public String getUserEmail() { return userEmail; }
    public LocalDateTime getCheckinAt() { return checkinAt; }
    public LocalDateTime getWinnerDrawnAt() { return winnerDrawnAt; }
    public String getMessage() { return message; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}
