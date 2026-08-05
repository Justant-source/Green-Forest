package com.vgc.util;

import java.time.DateTimeException;
import java.time.LocalDate;

public final class BirthMonthDay {

    private BirthMonthDay() {}

    public static void requireValid(Integer month, Integer day) {
        if (month == null || day == null) {
            throw new RuntimeException("생일을 입력해주세요.");
        }
        if (month < 1 || month > 12) {
            throw new RuntimeException("생일 월이 올바르지 않습니다.");
        }
        int maxDay = maxDayOfMonth(month);
        if (day < 1 || day > maxDay) {
            throw new RuntimeException("생일 일이 올바르지 않습니다.");
        }
    }

    public static int maxDayOfMonth(int month) {
        return switch (month) {
            case 2 -> 29;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }

    /** 올해(또는 내년) 생일 날짜. 2/29는 비윤년이면 2/28. */
    public static LocalDate nextOccurrence(int month, int day, LocalDate today) {
        LocalDate candidate = safeDate(today.getYear(), month, day);
        if (candidate.isBefore(today)) {
            candidate = safeDate(today.getYear() + 1, month, day);
        }
        return candidate;
    }

    public static LocalDate thisYearOccurrence(int month, int day, int year) {
        return safeDate(year, month, day);
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            // 2/29 on non-leap year → 2/28
            return LocalDate.of(year, month, month == 2 ? 28 : day);
        }
    }

    public static String display(int month, int day) {
        return String.format("%02d/%02d", month, day);
    }
}
