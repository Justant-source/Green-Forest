package com.vgc.dto.request;

import java.util.ArrayList;
import java.util.List;

public class SnackGroupedPendingResponse {
    private Long catalogItemId;
    private String name;
    private String imageUrl;
    private String ssgUrl;
    private boolean available;
    private boolean hiddenFromUsers;
    private int requesterCount;
    private int totalQty;
    private List<Line> lines = new ArrayList<>();

    public Long getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(Long catalogItemId) { this.catalogItemId = catalogItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSsgUrl() { return ssgUrl; }
    public void setSsgUrl(String ssgUrl) { this.ssgUrl = ssgUrl; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isHiddenFromUsers() { return hiddenFromUsers; }
    public void setHiddenFromUsers(boolean hiddenFromUsers) { this.hiddenFromUsers = hiddenFromUsers; }
    public int getRequesterCount() { return requesterCount; }
    public void setRequesterCount(int requesterCount) { this.requesterCount = requesterCount; }
    public int getTotalQty() { return totalQty; }
    public void setTotalQty(int totalQty) { this.totalQty = totalQty; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }

    public static class Line {
        private Long id;
        private Long userId;
        private String nickname;
        private int quantity;
        private String createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
