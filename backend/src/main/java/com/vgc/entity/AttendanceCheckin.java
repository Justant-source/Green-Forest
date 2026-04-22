package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_checkins", indexes = {
    @Index(name = "idx_attendance_user_date", columnList = "user_id, checkin_date DESC"),
    @Index(name = "idx_attendance_date", columnList = "checkin_date DESC"),
    @Index(name = "idx_attendance_winner", columnList = "checkin_date, is_winner"),
    @Index(name = "idx_attendance_winner_delivery", columnList = "is_winner, delivery_status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_attendance_user_date", columnNames = {"user_id", "checkin_date"})
})
public class AttendanceCheckin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "checkin_at", nullable = false)
    private LocalDateTime checkinAt;

    @Column(length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private AttendanceMessageType messageType;

    @Column(name = "suggested_phrase_id")
    private Long suggestedPhraseId;

    @Column(name = "stamp_style", length = 30, nullable = false)
    private String stampStyle;

    @Column(name = "is_winner", nullable = false)
    private boolean winner = false;

    @Column(name = "winner_drawn_at")
    private LocalDateTime winnerDrawnAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private AttendanceDeliveryStatus deliveryStatus = AttendanceDeliveryStatus.NONE;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by")
    private Long deliveredBy;

    @Column(name = "delivery_memo", length = 500)
    private String deliveryMemo;

    @Column(name = "drops_awarded", nullable = false)
    private int dropsAwarded = 10;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }
    public LocalDateTime getCheckinAt() { return checkinAt; }
    public void setCheckinAt(LocalDateTime checkinAt) { this.checkinAt = checkinAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public AttendanceMessageType getMessageType() { return messageType; }
    public void setMessageType(AttendanceMessageType messageType) { this.messageType = messageType; }
    public Long getSuggestedPhraseId() { return suggestedPhraseId; }
    public void setSuggestedPhraseId(Long suggestedPhraseId) { this.suggestedPhraseId = suggestedPhraseId; }
    public String getStampStyle() { return stampStyle; }
    public void setStampStyle(String stampStyle) { this.stampStyle = stampStyle; }
    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }
    public LocalDateTime getWinnerDrawnAt() { return winnerDrawnAt; }
    public void setWinnerDrawnAt(LocalDateTime winnerDrawnAt) { this.winnerDrawnAt = winnerDrawnAt; }
    public AttendanceDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(AttendanceDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public Long getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(Long deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public void setDeliveryMemo(String deliveryMemo) { this.deliveryMemo = deliveryMemo; }
    public int getDropsAwarded() { return dropsAwarded; }
    public void setDropsAwarded(int dropsAwarded) { this.dropsAwarded = dropsAwarded; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
