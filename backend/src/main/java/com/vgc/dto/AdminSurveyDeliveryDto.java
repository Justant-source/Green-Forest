package com.vgc.dto;

import com.vgc.entity.SurveyDelivery;

import java.time.LocalDateTime;

public class AdminSurveyDeliveryDto {
    private Long id;
    private Long surveyId;
    private String surveyTitle;
    private Long userId;
    private String userNickname;
    private String userName;
    private Long optionId;
    private String optionText;
    private String optionImageUrl;
    private String recipientName;
    private String recipientPhone;
    private String recipientZipcode;
    private String recipientAddressMain;
    private String recipientAddressDetail;
    private String deliveryStatus;
    private String deliveryStatusLabel;
    private String trackingNumber;
    private LocalDateTime deliveredAt;
    private Long deliveredBy;
    private String deliveryMemo;
    private LocalDateTime createdAt;

    public static AdminSurveyDeliveryDto from(SurveyDelivery d) {
        AdminSurveyDeliveryDto dto = new AdminSurveyDeliveryDto();
        dto.id = d.getId();
        dto.surveyId = d.getSurvey().getId();
        dto.surveyTitle = d.getSurvey().getPost().getTitle();
        dto.userId = d.getUser().getId();
        dto.userNickname = d.getUser().getNickname();
        dto.userName = d.getUser().getName();
        dto.optionId = d.getOption().getId();
        dto.optionText = d.getOption().getTextContent();
        dto.optionImageUrl = d.getOption().getImageUrl();
        dto.recipientName = d.getRecipientName();
        dto.recipientPhone = d.getRecipientPhone();
        dto.recipientZipcode = d.getRecipientZipcode();
        dto.recipientAddressMain = d.getRecipientAddressMain();
        dto.recipientAddressDetail = d.getRecipientAddressDetail();
        dto.deliveryStatus = d.getDeliveryStatus().name();
        dto.deliveryStatusLabel = d.getDeliveryStatus().getLabel();
        dto.trackingNumber = d.getTrackingNumber();
        dto.deliveredAt = d.getDeliveredAt();
        dto.deliveredBy = d.getDeliveredBy();
        dto.deliveryMemo = d.getDeliveryMemo();
        dto.createdAt = d.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getSurveyId() { return surveyId; }
    public String getSurveyTitle() { return surveyTitle; }
    public Long getUserId() { return userId; }
    public String getUserNickname() { return userNickname; }
    public String getUserName() { return userName; }
    public Long getOptionId() { return optionId; }
    public String getOptionText() { return optionText; }
    public String getOptionImageUrl() { return optionImageUrl; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getRecipientZipcode() { return recipientZipcode; }
    public String getRecipientAddressMain() { return recipientAddressMain; }
    public String getRecipientAddressDetail() { return recipientAddressDetail; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public String getDeliveryStatusLabel() { return deliveryStatusLabel; }
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public Long getDeliveredBy() { return deliveredBy; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
