package com.vgc.service;

import com.vgc.dto.AdminSurveyDeliveryDto;
import com.vgc.entity.*;
import com.vgc.repository.SurveyDeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SurveyDeliveryService {

    private final SurveyDeliveryRepository deliveryRepository;

    public SurveyDeliveryService(SurveyDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public void createSnapshot(Survey survey, SurveyOption option, User user,
                               SurveyVote vote, String recipientNameOverride) {
        SurveyDelivery delivery = new SurveyDelivery();
        delivery.setSurvey(survey);
        delivery.setOption(option);
        delivery.setUser(user);
        delivery.setVote(vote);
        delivery.setRecipientName(
            (recipientNameOverride != null && !recipientNameOverride.isBlank())
                ? recipientNameOverride.trim()
                : user.getName()
        );
        delivery.setRecipientPhone(user.getPhone());
        delivery.setRecipientZipcode(user.getZipcode() != null ? user.getZipcode() : "");
        delivery.setRecipientAddressMain(user.getAddressMain());
        delivery.setRecipientAddressDetail(user.getAddressDetail());
        delivery.setDeliveryStatus(SurveyDeliveryStatus.PENDING);
        deliveryRepository.save(delivery);
    }

    @Transactional(readOnly = true)
    public List<AdminSurveyDeliveryDto> list(Long surveyId, SurveyDeliveryStatus status) {
        List<SurveyDelivery> deliveries = surveyId != null
            ? deliveryRepository.findBySurveyIdAndDeliveryStatusOrderByCreatedAtDesc(surveyId, status)
            : deliveryRepository.findByDeliveryStatusOrderByCreatedAtDesc(status);
        return deliveries.stream().map(AdminSurveyDeliveryDto::from).toList();
    }

    @Transactional
    public AdminSurveyDeliveryDto update(Long id, Long adminId,
                                         String status, String trackingNumber, String memo) {
        SurveyDelivery delivery = deliveryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        if (status != null) {
            SurveyDeliveryStatus newStatus = SurveyDeliveryStatus.valueOf(status);
            delivery.setDeliveryStatus(newStatus);
            if (newStatus == SurveyDeliveryStatus.DELIVERED) {
                delivery.setDeliveredAt(LocalDateTime.now());
                delivery.setDeliveredBy(adminId);
            }
        }
        if (trackingNumber != null) {
            delivery.setTrackingNumber(trackingNumber.isBlank() ? null : trackingNumber.trim());
        }
        if (memo != null) {
            delivery.setDeliveryMemo(memo.isBlank() ? null : memo.trim());
        }

        return AdminSurveyDeliveryDto.from(deliveryRepository.save(delivery));
    }
}
