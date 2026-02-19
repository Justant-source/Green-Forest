package com.vgc.dto;

import com.vgc.entity.Category;

public class CategoryResponse {
    private Long id;
    private String name;
    private String label;
    private String color;
    private boolean hasStatus;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        response.label = category.getLabel();
        response.color = category.getColor();
        response.hasStatus = category.isHasStatus();
        return response;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
    public boolean isHasStatus() { return hasStatus; }
}
