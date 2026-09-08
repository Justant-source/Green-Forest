package com.vgc.dto.request;

public class SnackSsgCategoryResponse {
    private String code;
    private String label;

    public SnackSsgCategoryResponse() {
    }

    public SnackSsgCategoryResponse(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
