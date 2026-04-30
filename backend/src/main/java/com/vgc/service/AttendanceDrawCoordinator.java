package com.vgc.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 출석 추첨 동시 실행 방지 조정자.
 *
 * @Scheduled 자동 추첨과 관리자 수동 추첨이 동시에 호출되면
 * READ COMMITTED 격리 수준에서 두 트랜잭션 모두 "당첨자 없음" 판정 후
 * 중복 저장(레이스 컨디션)이 발생한다.
 *
 * attendanceService 는 Spring 프록시이므로
 * draw() 호출 → 프록시가 트랜잭션 시작 → 메서드 실행 → 커밋 → 반환
 * 순서가 보장된다. lock.unlock()은 커밋 완료 이후에 실행되므로
 * 후속 쓰레드는 이미 커밋된 당첨자를 보고 skip된다.
 */
@Component
public class AttendanceDrawCoordinator {

    private final AttendanceService attendanceService;
    private final ReentrantLock lock = new ReentrantLock();

    public AttendanceDrawCoordinator(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    public Map<String, Object> draw(LocalDate date) {
        if (!lock.tryLock()) {
            return Map.of("skipped", true, "reason", "추첨이 이미 진행 중입니다");
        }
        try {
            return attendanceService.drawDailyWinner(date);
        } finally {
            lock.unlock();
        }
    }
}
