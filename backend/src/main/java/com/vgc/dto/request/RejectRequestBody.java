package com.vgc.dto.request;

import com.vgc.entity.RequestRejectReason;

public class RejectRequestBody {
    private RequestRejectReason reason;
    private String note;

    public RequestRejectReason getReason() { return reason; }
    public void setReason(RequestRejectReason reason) { this.reason = reason; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
