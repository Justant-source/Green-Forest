package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quests", indexes = {
    @Index(name = "idx_quests_active_dates", columnList = "is_active, start_date, end_date"),
    @Index(name = "idx_quests_created_by", columnList = "created_by"),
    @Index(name = "idx_quests_target_party", columnList = "target_party_id")
})
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reward_drops", nullable = false)
    private int rewardDrops = 50;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "target_type", nullable = false, length = 10)
    private String targetType = "전체"; // 전체, 파티별

    @Column(name = "target_party_id")
    private Long targetPartyId;

    @Column(name = "max_completions_per_user", nullable = false)
    private int maxCompletionsPerUser = 1;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_vote_type", nullable = false)
    private boolean voteType = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRewardDrops() { return rewardDrops; }
    public void setRewardDrops(int rewardDrops) { this.rewardDrops = rewardDrops; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetPartyId() { return targetPartyId; }
    public void setTargetPartyId(Long targetPartyId) { this.targetPartyId = targetPartyId; }
    public int getMaxCompletionsPerUser() { return maxCompletionsPerUser; }
    public void setMaxCompletionsPerUser(int maxCompletionsPerUser) { this.maxCompletionsPerUser = maxCompletionsPerUser; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isVoteType() { return voteType; }
    public void setVoteType(boolean voteType) { this.voteType = voteType; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
