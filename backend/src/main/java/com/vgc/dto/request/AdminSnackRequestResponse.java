package com.vgc.dto.request;

import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;

public class AdminSnackRequestResponse {
    private Long id;
    private Long catalogItemId;
    private String name;
    private String imageUrl;
    private String ssgUrl;
    private int quantity;
    private RequestStatus status;
    private RequestRejectReason rejectReason;
    private String rejectReasonLabel;
    private String rejectNote;
    private String createdAt;
    private String purchasedAt;
    private String rejectedAt;
    private Long userId;
    private String nickname;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(Long catalogItemId) { this.catalogItemId = catalogItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSsgUrl() { return ssgUrl; }
    public void setSsgUrl(String ssgUrl) { this.ssgUrl = ssgUrl; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public RequestRejectReason getRejectReason() { return rejectReason; }
    public void setRejectReason(RequestRejectReason rejectReason) { this.rejectReason = rejectReason; }
    public String getRejectReasonLabel() { return rejectReasonLabel; }
    public void setRejectReasonLabel(String rejectReasonLabel) { this.rejectReasonLabel = rejectReasonLabel; }
    public String getRejectNote() { return rejectNote; }
    public void setRejectNote(String rejectNote) { this.rejectNote = rejectNote; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(String purchasedAt) { this.purchasedAt = purchasedAt; }
    public String getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(String rejectedAt) { this.rejectedAt = rejectedAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
