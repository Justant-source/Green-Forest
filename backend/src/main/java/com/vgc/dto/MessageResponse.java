package com.vgc.dto;

import com.vgc.entity.Message;

import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private Long conversationId;
    private String senderNickname;
    private String content;
    private boolean systemMessage;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        MessageResponse res = new MessageResponse();
        res.id = message.getId();
        res.conversationId = message.getConversation().getId();
        res.senderNickname = message.getSender() != null ? message.getSender().getNickname() : null;
        res.content = message.getContent();
        res.systemMessage = message.isSystemMessage();
        res.createdAt = message.getCreatedAt();
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isSystemMessage() { return systemMessage; }
    public void setSystemMessage(boolean systemMessage) { this.systemMessage = systemMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
