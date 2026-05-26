package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_deliveries", indexes = {
    @Index(name = "idx_sd_survey_status", columnList = "survey_id, delivery_status"),
    @Index(name = "idx_sd_user", columnList = "user_id, created_at")
})
public class SurveyDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private SurveyOption option;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", unique = true)
    private SurveyVote vote;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "recipient_zipcode", nullable = false, length = 10)
    private String recipientZipcode;

    @Column(name = "recipient_address_main", nullable = false, length = 200)
    private String recipientAddressMain;

    @Column(name = "recipient_address_detail", length = 200)
    private String recipientAddressDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private SurveyDeliveryStatus deliveryStatus = SurveyDeliveryStatus.PENDING;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by")
    private Long deliveredBy;

    @Column(name = "delivery_memo", length = 500)
    private String deliveryMemo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public SurveyOption getOption() { return option; }
    public void setOption(SurveyOption option) { this.option = option; }
    public SurveyVote getVote() { return vote; }
    public void setVote(SurveyVote vote) { this.vote = vote; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getRecipientZipcode() { return recipientZipcode; }
    public void setRecipientZipcode(String recipientZipcode) { this.recipientZipcode = recipientZipcode; }
    public String getRecipientAddressMain() { return recipientAddressMain; }
    public void setRecipientAddressMain(String recipientAddressMain) { this.recipientAddressMain = recipientAddressMain; }
    public String getRecipientAddressDetail() { return recipientAddressDetail; }
    public void setRecipientAddressDetail(String recipientAddressDetail) { this.recipientAddressDetail = recipientAddressDetail; }
    public SurveyDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(SurveyDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public Long getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(Long deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public void setDeliveryMemo(String deliveryMemo) { this.deliveryMemo = deliveryMemo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
