package com.vgc.dto;

import com.vgc.entity.WeeklyReport;
import java.time.LocalDate;

public class WeeklyReportDto {
    private Long id;
    private Long userId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int earnedAmount;
    private Integer partyRank;

    public static WeeklyReportDto from(WeeklyReport report) {
        WeeklyReportDto dto = new WeeklyReportDto();
        dto.id = report.getId();
        dto.userId = report.getUser().getId();
        dto.weekStart = report.getWeekStart();
        dto.weekEnd = report.getWeekEnd();
        dto.earnedAmount = report.getEarnedAmount();
        dto.partyRank = report.getPartyRank();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }

    public int getEarnedAmount() { return earnedAmount; }
    public void setEarnedAmount(int earnedAmount) { this.earnedAmount = earnedAmount; }

    public Integer getPartyRank() { return partyRank; }
    public void setPartyRank(Integer partyRank) { this.partyRank = partyRank; }
}
