package com.vgc.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WeeklyReportScheduler {
    private final WeeklyReportService weeklyReportService;

    public WeeklyReportScheduler(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
    public void generateWeeklyReports() {
        weeklyReportService.generateWeeklyReports(LocalDate.now());
    }
}
