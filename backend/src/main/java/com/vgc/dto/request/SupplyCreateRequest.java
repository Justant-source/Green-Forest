package com.vgc.dto.request;

public class SupplyCreateRequest {
    private String title;
    private int quantity = 1;
    private String memo;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
