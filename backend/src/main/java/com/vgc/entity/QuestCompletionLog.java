package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quest_completion_log",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_quest_completion",
            columnNames = {"user_id", "quest_type", "category", "period_key"}
        )
    },
    indexes = {
        @Index(name = "idx_quest_log_user", columnList = "user_id"),
        @Index(name = "idx_quest_log_period", columnList = "quest_type, period_key")
    }
)
public class QuestCompletionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quest_type", nullable = false, length = 10)
    private String questType; // 일일, 주간, 월간

    @Column(nullable = false, length = 20)
    private String category; // 긍정문구, 동료칭찬, 야외인증

    @Column(name = "period_key", nullable = false, length = 20)
    private String periodKey; // 일일: 2026-03-18, 주간: 2026-W12, 월간: 2026-03

    @Column(name = "post_id", nullable = false)
    private Long postId;

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
    public String getQuestType() { return questType; }
    public void setQuestType(String questType) { this.questType = questType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
