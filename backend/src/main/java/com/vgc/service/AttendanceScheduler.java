package com.vgc.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    public AttendanceScheduler(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Scheduled(cron = "0 0 11 * * MON-FRI", zone = "Asia/Seoul")
    public void drawDailyWinner() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        attendanceService.drawDailyWinner(today);
    }
}
