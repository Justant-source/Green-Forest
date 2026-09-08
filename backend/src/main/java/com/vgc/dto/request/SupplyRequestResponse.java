package com.vgc.dto.request;

import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;

public class SupplyRequestResponse {
    private Long id;
    private String title;
    private int quantity;
    private String memo;
    private RequestStatus status;
    private RequestRejectReason rejectReason;
    private String rejectReasonLabel;
    private String rejectNote;
    private String createdAt;
    private String purchasedAt;
    private String rejectedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
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
}
