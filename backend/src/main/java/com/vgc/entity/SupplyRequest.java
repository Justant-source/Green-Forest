package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_requests", indexes = {
    @Index(name = "idx_supply_req_status_created", columnList = "status, created_at"),
    @Index(name = "idx_supply_req_user", columnList = "user_id, created_at")
})
public class SupplyRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 300)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 20)
    private RequestRejectReason rejectReason;

    @Column(name = "reject_note", length = 100)
    private String rejectNote;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public RequestRejectReason getRejectReason() { return rejectReason; }
    public void setRejectReason(RequestRejectReason rejectReason) { this.rejectReason = rejectReason; }
    public String getRejectNote() { return rejectNote; }
    public void setRejectNote(String rejectNote) { this.rejectNote = rejectNote; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
