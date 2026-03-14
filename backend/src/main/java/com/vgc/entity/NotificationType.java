package com.vgc.entity;

public enum NotificationType {
    TAG("태깅"),
    COMMENT("댓글"),
    DROP_AWARD("물방울지급"),
    QUEST_CREATED("퀘스트생성"),
    QUEST_DEADLINE("퀘스트마감임박"),
    HAZARD("위해요소"),
    ANNOUNCEMENT("공지");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
