package com.vgc.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class AttendanceScheduler {

    private final AttendanceDrawCoordinator coordinator;

    public AttendanceScheduler(AttendanceDrawCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(cron = "0 0 11 * * MON-FRI", zone = "Asia/Seoul")
    public void drawDailyWinner() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        coordinator.draw(today);
    }
}
