package com.vgc.entity;

public enum GrowthScoreReason {
    ATTENDANCE_DAILY,         // +2  일 1회
    ATTENDANCE_STREAK_7,      // +5  평생 1회
    ATTENDANCE_STREAK_30,     // +15 평생 1회
    ATTENDANCE_STREAK_100,    // +50 평생 1회
    ATTENDANCE_DRAW_WIN,      // +10 추첨 당첨
    POST_CREATED,             // +2  일 +6 캡 (본문 30자 이상)
    LIKE_RECEIVED,            // +1  일 +10 캡
    COMMENT_RECEIVED,         // +3  일 +15 캡
    PRAISE_RECEIVED,          // +10 일 +20 캡
    GACHA_WIN,                // +5
    ONBOARDING_FIRST_POST,    // +5 평생 1회 (30자 이상 첫 게시글)
    ONBOARDING_FIRST_COMMENT, // +5 평생 1회 (첫 댓글 — 자기 글 포함)
    ADMIN_ADJUSTMENT          // 음수 가능 (어뷰징 회수)
}
