package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plant_growth", indexes = {
    @Index(name = "idx_plant_growth_user", columnList = "user_id", unique = true)
})
public class PlantGrowth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int stage = 0;

    @Column(name = "growth_score", nullable = false)
    private int growthScore = 0;

    @Column(name = "likes_received", nullable = false)
    private int likesReceived = 0;

    @Column(name = "comments_received", nullable = false)
    private int commentsReceived = 0;

    @Column(name = "praises_received", nullable = false)
    private int praisesReceived = 0;

    @Column(name = "last_grown_at")
    private LocalDateTime lastGrownAt;

    @Column(name = "last_stage_up_score", nullable = false)
    private int lastStageUpScore = 0;

    @Column(name = "attendance_score", nullable = false)
    private int attendanceScore = 0;

    @Column(name = "post_score", nullable = false)
    private int postScore = 0;

    @Column(name = "gacha_score", nullable = false)
    private int gachaScore = 0;

    @Column(name = "onboarding_score", nullable = false)
    private int onboardingScore = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
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
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public int getGrowthScore() { return growthScore; }
    public void setGrowthScore(int growthScore) { this.growthScore = growthScore; }
    public int getLikesReceived() { return likesReceived; }
    public void setLikesReceived(int likesReceived) { this.likesReceived = likesReceived; }
    public int getCommentsReceived() { return commentsReceived; }
    public void setCommentsReceived(int commentsReceived) { this.commentsReceived = commentsReceived; }
    public int getPraisesReceived() { return praisesReceived; }
    public void setPraisesReceived(int praisesReceived) { this.praisesReceived = praisesReceived; }
    public LocalDateTime getLastGrownAt() { return lastGrownAt; }
    public void setLastGrownAt(LocalDateTime lastGrownAt) { this.lastGrownAt = lastGrownAt; }
    public int getLastStageUpScore() { return lastStageUpScore; }
    public void setLastStageUpScore(int lastStageUpScore) { this.lastStageUpScore = lastStageUpScore; }
    public int getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(int attendanceScore) { this.attendanceScore = attendanceScore; }
    public int getPostScore() { return postScore; }
    public void setPostScore(int postScore) { this.postScore = postScore; }
    public int getGachaScore() { return gachaScore; }
    public void setGachaScore(int gachaScore) { this.gachaScore = gachaScore; }
    public int getOnboardingScore() { return onboardingScore; }
    public void setOnboardingScore(int onboardingScore) { this.onboardingScore = onboardingScore; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
