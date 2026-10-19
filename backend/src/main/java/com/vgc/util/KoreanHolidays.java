package com.vgc.util;

import java.time.LocalDate;
import java.util.Set;

/**
 * 대한민국 법정 공휴일(대체공휴일·임시공휴일 포함). 평일에 해당하는 날만 적는다.
 * 목록에 없는 연도는 공휴일이 없는 것으로 본다(출석을 막지 않는 쪽으로 실패).
 * 새 해가 되면 해당 연도 날짜를 추가해야 한다.
 */
public final class KoreanHolidays {

    private static final Set<LocalDate> WEEKDAY_HOLIDAYS = Set.of(
            // 2026
            LocalDate.of(2026, 1, 1),   // 신정
            LocalDate.of(2026, 2, 16),  // 설날 연휴
            LocalDate.of(2026, 2, 17),  // 설날
            LocalDate.of(2026, 2, 18),  // 설날 연휴
            LocalDate.of(2026, 3, 2),   // 3·1절 대체공휴일
            LocalDate.of(2026, 5, 5),   // 어린이날
            LocalDate.of(2026, 5, 25),  // 부처님오신날 대체공휴일
            LocalDate.of(2026, 6, 3),   // 전국동시지방선거
            LocalDate.of(2026, 7, 17),  // 제헌절
            LocalDate.of(2026, 8, 17),  // 광복절 대체공휴일
            LocalDate.of(2026, 9, 24),  // 추석 연휴
            LocalDate.of(2026, 9, 25),  // 추석
            LocalDate.of(2026, 10, 5),  // 개천절 대체공휴일
            LocalDate.of(2026, 10, 9),  // 한글날
            LocalDate.of(2026, 12, 25)  // 성탄절
    );

    private KoreanHolidays() {}

    public static boolean isHoliday(LocalDate date) {
        return WEEKDAY_HOLIDAYS.contains(date);
    }
}
