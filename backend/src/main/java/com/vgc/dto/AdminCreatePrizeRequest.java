package com.vgc.dto;

import com.vgc.entity.GachaPrizeTier;
import java.math.BigDecimal;

public class AdminCreatePrizeRequest {
    private String name;
    private String description;
    private String imageUrl;
    private int cashValue;
    private int totalStock;
    private GachaPrizeTier tier;
    private BigDecimal evMultiplier;
    private int displayOrder;

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

    public int getCashValue() {
        return cashValue;
    }

    public void setCashValue(int cashValue) {
        this.cashValue = cashValue;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public GachaPrizeTier getTier() {
        return tier;
    }

    public void setTier(GachaPrizeTier tier) {
        this.tier = tier;
    }

    public BigDecimal getEvMultiplier() {
        return evMultiplier;
    }

    public void setEvMultiplier(BigDecimal evMultiplier) {
        this.evMultiplier = evMultiplier;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
