package com.vgc.dto.request;

public class SnackCatalogCardResponse {
    private Long id;
    private String name;
    private String category;
    private String categoryLabel;
    private String imageUrl;
    private String ssgUrl;
    private int pendingQty;
    private int myPendingQty;
    private Long myRequestId;
    private boolean available;
    private boolean hiddenFromUsers;
    private boolean canRequest;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSsgUrl() { return ssgUrl; }
    public void setSsgUrl(String ssgUrl) { this.ssgUrl = ssgUrl; }
    public int getPendingQty() { return pendingQty; }
    public void setPendingQty(int pendingQty) { this.pendingQty = pendingQty; }
    public int getMyPendingQty() { return myPendingQty; }
    public void setMyPendingQty(int myPendingQty) { this.myPendingQty = myPendingQty; }
    public Long getMyRequestId() { return myRequestId; }
    public void setMyRequestId(Long myRequestId) { this.myRequestId = myRequestId; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isHiddenFromUsers() { return hiddenFromUsers; }
    public void setHiddenFromUsers(boolean hiddenFromUsers) { this.hiddenFromUsers = hiddenFromUsers; }
    public boolean isCanRequest() { return canRequest; }
    public void setCanRequest(boolean canRequest) { this.canRequest = canRequest; }
}
