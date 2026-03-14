package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quest_completions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_quest_user_post", columnNames = {"quest_id", "user_id", "post_id"})
    },
    indexes = {
        @Index(name = "idx_quest_comp_quest_user", columnList = "quest_id, user_id"),
        @Index(name = "idx_quest_comp_user", columnList = "user_id"),
        @Index(name = "idx_quest_comp_post", columnList = "post_id")
    }
)
public class QuestCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.completedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Quest getQuest() { return quest; }
    public void setQuest(Quest quest) { this.quest = quest; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
