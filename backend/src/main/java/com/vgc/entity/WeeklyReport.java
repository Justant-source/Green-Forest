package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_week", columnNames = {"user_id", "week_start"})
}, indexes = {
    @Index(name = "idx_weekly_reports_user", columnList = "user_id"),
    @Index(name = "idx_weekly_reports_week_start", columnList = "week_start"),
    @Index(name = "idx_weekly_reports_user_week_start", columnList = "user_id, week_start DESC")
})
public class WeeklyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "earned_amount", nullable = false)
    private int earnedAmount = 0;

    @Column(name = "party_rank")
    private Integer partyRank;

    @Column(name = "created_at", nullable = false)
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

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }

    public int getEarnedAmount() { return earnedAmount; }
    public void setEarnedAmount(int earnedAmount) { this.earnedAmount = earnedAmount; }

    public Integer getPartyRank() { return partyRank; }
    public void setPartyRank(Integer partyRank) { this.partyRank = partyRank; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
