package com.vgc.dto.request;

public class PurchaseCountResponse {
    private int updated;

    public PurchaseCountResponse() {
    }

    public PurchaseCountResponse(int updated) {
        this.updated = updated;
    }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }
}
