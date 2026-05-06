package com.vgc.service;

import com.vgc.entity.Announcement;
import com.vgc.entity.AnnouncementType;
import com.vgc.entity.User;
import com.vgc.repository.AnnouncementRepository;
import com.vgc.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BirthdayAnnouncementScheduler {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public BirthdayAnnouncementScheduler(AnnouncementRepository announcementRepository,
                                          UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void postBirthdayAnnouncements() {
        LocalDate today = LocalDate.now();

        // 기존 BIRTHDAY 공지 비활성화
        announcementRepository.deactivateAllByType(AnnouncementType.BIRTHDAY);

        // 오늘 생일인 사용자 조회
        List<User> birthdayUsers = userRepository.findByBirthMonthAndDay(
                today.getMonthValue(), today.getDayOfMonth());

        if (birthdayUsers.isEmpty()) return;

        String names = birthdayUsers.stream()
                .map(User::getName)
                .collect(Collectors.joining(", "));

        String title = birthdayUsers.size() == 1
                ? "🎉 오늘은 " + names + "님의 생일입니다!"
                : "🎉 오늘은 " + names + "님들의 생일입니다!";

        Announcement ann = new Announcement();
        ann.setTitle(title);
        ann.setContent(title);
        ann.setType(AnnouncementType.BIRTHDAY);
        ann.setActive(true);
        announcementRepository.save(ann);
    }
}
