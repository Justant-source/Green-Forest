package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_quest_user", columnNames = {"quest_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_votes_quest", columnList = "quest_id"),
        @Index(name = "idx_votes_voted_for_user", columnList = "voted_for_user_id")
    }
)
public class Vote {
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
    @JoinColumn(name = "voted_for_user_id")
    private User votedForUser;

    @Column(name = "voted_for_option", length = 200)
    private String votedForOption;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Quest getQuest() { return quest; }
    public void setQuest(Quest quest) { this.quest = quest; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getVotedForUser() { return votedForUser; }
    public void setVotedForUser(User votedForUser) { this.votedForUser = votedForUser; }
    public String getVotedForOption() { return votedForOption; }
    public void setVotedForOption(String votedForOption) { this.votedForOption = votedForOption; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
