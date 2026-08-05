package com.vgc.dto;

public class AdminAnnouncementRequest {
    private String title;
    private String content;
    private String type;
    private String relatedUrl;
    private String relatedLabel;
    private String expiresAt;
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public String getContent(){return content;} public void setContent(String value){content=value;}
    public String getType(){return type;} public void setType(String value){type=value;}
    public String getRelatedUrl(){return relatedUrl;} public void setRelatedUrl(String value){relatedUrl=value;}
    public String getRelatedLabel(){return relatedLabel;} public void setRelatedLabel(String value){relatedLabel=value;}
    public String getExpiresAt(){return expiresAt;} public void setExpiresAt(String value){expiresAt=value;}
}
