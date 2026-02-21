package com.vgc.dto;

import com.vgc.entity.Conversation;
import com.vgc.entity.User;

import java.time.LocalDateTime;

public class ConversationResponse {
    private Long id;
    private String otherNickname;
    private String lastMessage;
    private LocalDateTime updatedAt;
    private boolean otherLeft;

    public static ConversationResponse from(Conversation conversation, User currentUser, String lastMessage) {
        ConversationResponse res = new ConversationResponse();
        res.id = conversation.getId();

        boolean isUser1 = conversation.getUser1().getId().equals(currentUser.getId());
        User other = isUser1 ? conversation.getUser2() : conversation.getUser1();
        res.otherNickname = other.getNickname();
        res.otherLeft = isUser1 ? conversation.isUser2Left() : conversation.isUser1Left();
        res.lastMessage = lastMessage;
        res.updatedAt = conversation.getUpdatedAt();

        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOtherNickname() { return otherNickname; }
    public void setOtherNickname(String otherNickname) { this.otherNickname = otherNickname; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOtherLeft() { return otherLeft; }
    public void setOtherLeft(boolean otherLeft) { this.otherLeft = otherLeft; }
}
