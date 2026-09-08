package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "snack_catalog_items", indexes = {
    @Index(name = "idx_snack_cat_avail_hidden_order", columnList = "available, hidden_from_users, display_order"),
    @Index(name = "idx_snack_cat_category", columnList = "category")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_snack_cat_ssg_category", columnNames = {"ssg_item_id", "category"})
})
public class SnackCatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ssg_item_id", nullable = false, length = 40)
    private String ssgItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    private SnackSsgCategory category = SnackSsgCategory.SNACK;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "ssg_url", nullable = false, length = 500)
    private String ssgUrl;

    @Column(name = "ssg_image_url", length = 500)
    private String ssgImageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "hidden_from_users", nullable = false)
    private boolean hiddenFromUsers = false;

    @Column(name = "hidden_locked_by_admin", nullable = false)
    private boolean hiddenLockedByAdmin = false;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSsgItemId() { return ssgItemId; }
    public void setSsgItemId(String ssgItemId) { this.ssgItemId = ssgItemId; }
    public SnackSsgCategory getCategory() { return category; }
    public void setCategory(SnackSsgCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSsgUrl() { return ssgUrl; }
    public void setSsgUrl(String ssgUrl) { this.ssgUrl = ssgUrl; }
    public String getSsgImageUrl() { return ssgImageUrl; }
    public void setSsgImageUrl(String ssgImageUrl) { this.ssgImageUrl = ssgImageUrl; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isHiddenFromUsers() { return hiddenFromUsers; }
    public void setHiddenFromUsers(boolean hiddenFromUsers) { this.hiddenFromUsers = hiddenFromUsers; }
    public boolean isHiddenLockedByAdmin() { return hiddenLockedByAdmin; }
    public void setHiddenLockedByAdmin(boolean hiddenLockedByAdmin) { this.hiddenLockedByAdmin = hiddenLockedByAdmin; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
