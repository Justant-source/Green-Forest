package com.vgc.dto;

import com.vgc.entity.SurveyOptionType;
import org.springframework.web.multipart.MultipartFile;

public class SurveyOptionInput {
    private SurveyOptionType type;
    private String text;
    private MultipartFile image;

    public SurveyOptionType getType() { return type; }
    public void setType(SurveyOptionType type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
