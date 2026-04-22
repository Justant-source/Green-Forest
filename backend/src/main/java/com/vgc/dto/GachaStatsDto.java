package com.vgc.dto;

import java.math.BigDecimal;

public class GachaStatsDto {
    private long remaining;
    private int dailyLimit;
    private long todayDrawCount;
    private int betCost;
    private BigDecimal expectedReward;
    private int totalActivePrizes;

    public GachaStatsDto() {}

    public GachaStatsDto(long remaining, int dailyLimit, long todayDrawCount, int betCost,
                        BigDecimal expectedReward, int totalActivePrizes) {
        this.remaining = remaining;
        this.dailyLimit = dailyLimit;
        this.todayDrawCount = todayDrawCount;
        this.betCost = betCost;
        this.expectedReward = expectedReward;
        this.totalActivePrizes = totalActivePrizes;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public long getTodayDrawCount() {
        return todayDrawCount;
    }

    public void setTodayDrawCount(long todayDrawCount) {
        this.todayDrawCount = todayDrawCount;
    }

    public int getBetCost() {
        return betCost;
    }

    public void setBetCost(int betCost) {
        this.betCost = betCost;
    }

    public BigDecimal getExpectedReward() {
        return expectedReward;
    }

    public void setExpectedReward(BigDecimal expectedReward) {
        this.expectedReward = expectedReward;
    }

    public int getTotalActivePrizes() {
        return totalActivePrizes;
    }

    public void setTotalActivePrizes(int totalActivePrizes) {
        this.totalActivePrizes = totalActivePrizes;
    }
}
