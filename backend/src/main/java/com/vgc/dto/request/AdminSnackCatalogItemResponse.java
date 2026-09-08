package com.vgc.dto.request;

import java.time.LocalDateTime;

public class AdminSnackCatalogItemResponse {
    private Long id;
    private String ssgItemId;
    private String category;
    private String categoryLabel;
    private String name;
    private String imageUrl;
    private String ssgUrl;
    private String ssgImageUrl;
    private int displayOrder;
    private boolean available;
    private boolean hiddenFromUsers;
    private boolean hiddenLockedByAdmin;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int pendingQty;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSsgItemId() { return ssgItemId; }
    public void setSsgItemId(String ssgItemId) { this.ssgItemId = ssgItemId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
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
    public int getPendingQty() { return pendingQty; }
    public void setPendingQty(int pendingQty) { this.pendingQty = pendingQty; }
}
