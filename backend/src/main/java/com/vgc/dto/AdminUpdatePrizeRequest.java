package com.vgc.dto;

import java.math.BigDecimal;

public class AdminUpdatePrizeRequest {
    private String name;
    private String description;
    private String imageUrl;
    private Integer cashValue;
    private Integer totalStock;
    private Integer remainingStock;
    private BigDecimal evMultiplier;
    private Boolean active;
    private Integer displayOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getCashValue() {
        return cashValue;
    }

    public void setCashValue(Integer cashValue) {
        this.cashValue = cashValue;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(Integer remainingStock) {
        this.remainingStock = remainingStock;
    }

    public BigDecimal getEvMultiplier() {
        return evMultiplier;
    }

    public void setEvMultiplier(BigDecimal evMultiplier) {
        this.evMultiplier = evMultiplier;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
