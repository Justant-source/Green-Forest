package com.vgc.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 운영(policy) 시각 기준: Asia/Seoul.
 * DB DATETIME 감사 컬럼 기록은 JVM/JDBC 경로를 유지하고,
 * 일·주·월 경계·종료 시각 등 비즈니스 판정만 이 유틸을 사용한다.
 */
public final class AppTime {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private AppTime() {}

    public static LocalDate todayKst() {
        return LocalDate.now(KST);
    }

    public static LocalDateTime nowKst() {
        return LocalDateTime.now(KST);
    }

    public static YearMonth currentMonthKst() {
        return YearMonth.now(KST);
    }

    /** KST 달력일 [start, end) — created_at 등 일 단위 집계용 */
    public static LocalDateTime[] kstDayRange(LocalDate kstDate) {
        LocalDateTime start = kstDate.atStartOfDay();
        return new LocalDateTime[]{start, start.plusDays(1)};
    }

    public static LocalDateTime[] todayKstDayRange() {
        return kstDayRange(todayKst());
    }

    /** KST 달력월 [start, end) */
    public static LocalDateTime[] kstMonthRange(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return new LocalDateTime[]{start, end};
    }
}
