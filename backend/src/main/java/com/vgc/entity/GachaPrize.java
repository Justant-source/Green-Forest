package com.vgc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gacha_prizes", indexes = {
    @Index(name = "idx_prize_active_order", columnList = "is_active, display_order")
})
public class GachaPrize {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cash_value", nullable = false)
    private int cashValue;

    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Column(name = "remaining_stock", nullable = false)
    private int remainingStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GachaPrizeTier tier;

    @Column(name = "ev_multiplier", precision = 4, scale = 2, nullable = false)
    private BigDecimal evMultiplier = new BigDecimal("1.00");

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getCashValue() { return cashValue; }
    public void setCashValue(int cashValue) { this.cashValue = cashValue; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public int getRemainingStock() { return remainingStock; }
    public void setRemainingStock(int remainingStock) { this.remainingStock = remainingStock; }
    public GachaPrizeTier getTier() { return tier; }
    public void setTier(GachaPrizeTier tier) { this.tier = tier; }
    public BigDecimal getEvMultiplier() { return evMultiplier; }
    public void setEvMultiplier(BigDecimal evMultiplier) { this.evMultiplier = evMultiplier; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
