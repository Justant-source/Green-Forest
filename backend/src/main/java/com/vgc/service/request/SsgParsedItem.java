package com.vgc.service.request;

import com.vgc.entity.SnackSsgCategory;

public class SsgParsedItem {
    private String ssgItemId;
    private String name;
    private String ssgUrl;
    private String imageUrl;
    private SnackSsgCategory category = SnackSsgCategory.SNACK;

    public SsgParsedItem() {
    }

    public SsgParsedItem(String ssgItemId, String name, String ssgUrl, String imageUrl) {
        this(ssgItemId, name, ssgUrl, imageUrl, SnackSsgCategory.SNACK);
    }

    public SsgParsedItem(String ssgItemId, String name, String ssgUrl, String imageUrl,
                         SnackSsgCategory category) {
        this.ssgItemId = ssgItemId;
        this.name = name;
        this.ssgUrl = ssgUrl;
        this.imageUrl = imageUrl;
        this.category = category == null ? SnackSsgCategory.SNACK : category;
    }

    public String getSsgItemId() {
        return ssgItemId;
    }

    public void setSsgItemId(String ssgItemId) {
        this.ssgItemId = ssgItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSsgUrl() {
        return ssgUrl;
    }

    public void setSsgUrl(String ssgUrl) {
        this.ssgUrl = ssgUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public SnackSsgCategory getCategory() {
        return category;
    }

    public void setCategory(SnackSsgCategory category) {
        this.category = category == null ? SnackSsgCategory.SNACK : category;
    }
}
