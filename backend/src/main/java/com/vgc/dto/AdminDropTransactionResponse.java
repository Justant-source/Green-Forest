package com.vgc.dto;

import com.vgc.entity.DropTransaction;

import java.time.LocalDateTime;

public class AdminDropTransactionResponse {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userName;
    private int amount;
    private String reasonType;
    private String reasonLabel;
    private String reasonDetail;
    private Long relatedPostId;
    private Long relatedQuestId;
    private LocalDateTime createdAt;

    public static AdminDropTransactionResponse from(DropTransaction tx) {
        AdminDropTransactionResponse r = new AdminDropTransactionResponse();
        r.id = tx.getId();
        if (tx.getUser() != null) {
            r.userId = tx.getUser().getId();
            r.userNickname = tx.getUser().getNickname();
            r.userName = tx.getUser().getName();
        }
        r.amount = tx.getAmount();
        r.reasonType = tx.getReasonType() != null ? tx.getReasonType().name() : null;
        r.reasonLabel = tx.getReasonType() != null ? tx.getReasonType().getLabel() : null;
        r.reasonDetail = tx.getReasonDetail();
        r.relatedPostId = tx.getRelatedPostId();
        r.relatedQuestId = tx.getRelatedQuestId();
        r.createdAt = tx.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserNickname() { return userNickname; }
    public String getUserName() { return userName; }
    public int getAmount() { return amount; }
    public String getReasonType() { return reasonType; }
    public String getReasonLabel() { return reasonLabel; }
    public String getReasonDetail() { return reasonDetail; }
    public Long getRelatedPostId() { return relatedPostId; }
    public Long getRelatedQuestId() { return relatedQuestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
