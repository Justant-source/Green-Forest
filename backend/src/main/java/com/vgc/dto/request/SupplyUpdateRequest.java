package com.vgc.dto.request;

public class SupplyUpdateRequest {
    private String title;
    private Integer quantity;
    private String memo;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
