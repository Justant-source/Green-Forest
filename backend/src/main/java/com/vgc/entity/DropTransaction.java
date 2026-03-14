package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drop_transactions", indexes = {
    @Index(name = "idx_drop_tx_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_drop_tx_user_reason", columnList = "user_id, reason_type"),
    @Index(name = "idx_drop_tx_created", columnList = "created_at DESC"),
    @Index(name = "idx_drop_tx_related_post", columnList = "related_post_id"),
    @Index(name = "idx_drop_tx_related_quest", columnList = "related_quest_id")
})
public class DropTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private DropReasonType reasonType;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "related_post_id")
    private Long relatedPostId;

    @Column(name = "related_quest_id")
    private Long relatedQuestId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public DropReasonType getReasonType() { return reasonType; }
    public void setReasonType(DropReasonType reasonType) { this.reasonType = reasonType; }
    public String getReasonDetail() { return reasonDetail; }
    public void setReasonDetail(String reasonDetail) { this.reasonDetail = reasonDetail; }
    public Long getRelatedPostId() { return relatedPostId; }
    public void setRelatedPostId(Long relatedPostId) { this.relatedPostId = relatedPostId; }
    public Long getRelatedQuestId() { return relatedQuestId; }
    public void setRelatedQuestId(Long relatedQuestId) { this.relatedQuestId = relatedQuestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
