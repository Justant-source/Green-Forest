package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "growth_score_log", indexes = {
    @Index(name = "idx_gsl_user_date",       columnList = "user_id, created_at"),
    @Index(name = "idx_gsl_user_reason",      columnList = "user_id, reason"),
    @Index(name = "idx_gsl_user_reason_ref",  columnList = "user_id, reason, ref_id")
})
public class GrowthScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrowthScoreReason reason;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getScoreDelta() { return scoreDelta; }
    public void setScoreDelta(int scoreDelta) { this.scoreDelta = scoreDelta; }
    public GrowthScoreReason getReason() { return reason; }
    public void setReason(GrowthScoreReason reason) { this.reason = reason; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
