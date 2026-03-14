package com.vgc.dto;

import java.util.List;

public class PostRequest {
    private String title;
    private String content;
    private String category;
    private Long questId;
    private boolean anonymous;
    private List<String> taggedNicknames;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getQuestId() { return questId; }
    public void setQuestId(Long questId) { this.questId = questId; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public List<String> getTaggedNicknames() { return taggedNicknames; }
    public void setTaggedNicknames(List<String> taggedNicknames) { this.taggedNicknames = taggedNicknames; }
}
