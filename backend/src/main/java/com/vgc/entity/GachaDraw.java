package com.vgc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gacha_draws", indexes = {
    @Index(name = "idx_draw_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_draw_winner_delivery", columnList = "is_winner, delivery_status"),
    @Index(name = "idx_draw_prize", columnList = "prize_id")
})
public class GachaDraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id", nullable = false)
    private GachaPrize prize;

    @Column(name = "prize_name", nullable = false, length = 100)
    private String prizeName;

    @Column(name = "prize_cash_value", nullable = false)
    private int prizeCashValue;

    @Column(name = "drops_spent", nullable = false)
    private int dropsSpent;

    @Column(name = "win_probability", precision = 7, scale = 5, nullable = false)
    private BigDecimal winProbability;

    @Column(name = "rng_value", precision = 7, scale = 5, nullable = false)
    private BigDecimal rngValue;

    @Column(name = "is_winner", nullable = false)
    private boolean winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private GachaDeliveryStatus deliveryStatus = GachaDeliveryStatus.NONE;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by")
    private Long deliveredBy;

    @Column(name = "delivery_memo", length = 500)
    private String deliveryMemo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public GachaPrize getPrize() { return prize; }
    public void setPrize(GachaPrize prize) { this.prize = prize; }
    public String getPrizeName() { return prizeName; }
    public void setPrizeName(String prizeName) { this.prizeName = prizeName; }
    public int getPrizeCashValue() { return prizeCashValue; }
    public void setPrizeCashValue(int prizeCashValue) { this.prizeCashValue = prizeCashValue; }
    public int getDropsSpent() { return dropsSpent; }
    public void setDropsSpent(int dropsSpent) { this.dropsSpent = dropsSpent; }
    public BigDecimal getWinProbability() { return winProbability; }
    public void setWinProbability(BigDecimal winProbability) { this.winProbability = winProbability; }
    public BigDecimal getRngValue() { return rngValue; }
    public void setRngValue(BigDecimal rngValue) { this.rngValue = rngValue; }
    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }
    public GachaDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(GachaDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public Long getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(Long deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public void setDeliveryMemo(String deliveryMemo) { this.deliveryMemo = deliveryMemo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
