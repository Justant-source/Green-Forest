package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_tag_user", columnNames = {"post_id", "tagged_user_id"})
    },
    indexes = {
        @Index(name = "idx_post_tags_tagged_user", columnList = "tagged_user_id"),
        @Index(name = "idx_post_tags_post", columnList = "post_id")
    }
)
public class PostTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagged_user_id", nullable = false)
    private User taggedUser;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getTaggedUser() { return taggedUser; }
    public void setTaggedUser(User taggedUser) { this.taggedUser = taggedUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
