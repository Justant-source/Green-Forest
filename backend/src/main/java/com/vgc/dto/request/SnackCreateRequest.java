package com.vgc.dto.request;

public class SnackCreateRequest {
    private Long catalogItemId;
    private int quantity = 1;

    public Long getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(Long catalogItemId) { this.catalogItemId = catalogItemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
