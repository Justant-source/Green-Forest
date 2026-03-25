package com.vgc.entity;

public enum DropReasonType {
    DAILY_QUEST("일일퀘스트"),
    WEEKLY_QUEST("주간퀘스트"),
    MONTHLY_QUEST("월간퀘스트"),
    EVENT_QUEST("이벤트퀘스트"),
    TAG_BONUS("태깅보너스"),
    GM_AWARD("GM수동지급"),
    GM_DEDUCT("GM수동차감"),
    LIKE_BONUS("좋아요보너스"),
    COMMENT_BONUS("댓글보너스"),
    GIFT_RECEIVED("선물받음"),
    GIFT_SENT("선물보냄");

    private final String label;

    DropReasonType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
