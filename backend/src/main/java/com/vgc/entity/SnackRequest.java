package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "snack_requests", indexes = {
    @Index(name = "idx_snack_req_item_status", columnList = "catalog_item_id, status"),
    @Index(name = "idx_snack_req_user_created", columnList = "user_id, created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_snack_req_pending", columnNames = {"catalog_item_id", "pending_owner_id"})
})
public class SnackRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private SnackCatalogItem catalogItem;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 20)
    private RequestRejectReason rejectReason;

    @Column(name = "reject_note", length = 100)
    private String rejectNote;

    @Column(name = "pending_owner_id")
    private Long pendingOwnerId;

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
        syncPendingOwner();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncPendingOwner();
    }

    void syncPendingOwner() {
        this.pendingOwnerId = status == RequestStatus.PENDING && user != null ? user.getId() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public SnackCatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(SnackCatalogItem catalogItem) { this.catalogItem = catalogItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public RequestRejectReason getRejectReason() { return rejectReason; }
    public void setRejectReason(RequestRejectReason rejectReason) { this.rejectReason = rejectReason; }
    public String getRejectNote() { return rejectNote; }
    public void setRejectNote(String rejectNote) { this.rejectNote = rejectNote; }
    public Long getPendingOwnerId() { return pendingOwnerId; }
    public void setPendingOwnerId(Long pendingOwnerId) { this.pendingOwnerId = pendingOwnerId; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
