package com.vgc.entity.event.photobingo;

import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photo_bingo_submissions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pbs_event_user", columnNames = {"event_id", "user_id"})
    })
public class PhotoBingoSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000)
    private String caption;

    @Column(name = "final_reward_drops", nullable = false)
    private int finalRewardDrops = 0;

    @Column(name = "achieved_lines", nullable = false)
    private int achievedLines = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PhotoBingoCell> cells = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public int getFinalRewardDrops() { return finalRewardDrops; }
    public void setFinalRewardDrops(int finalRewardDrops) { this.finalRewardDrops = finalRewardDrops; }
    public int getAchievedLines() { return achievedLines; }
    public void setAchievedLines(int achievedLines) { this.achievedLines = achievedLines; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<PhotoBingoCell> getCells() { return cells; }
    public void setCells(List<PhotoBingoCell> cells) { this.cells = cells; }
}
