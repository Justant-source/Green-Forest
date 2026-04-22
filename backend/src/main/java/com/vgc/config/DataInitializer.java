package com.vgc.config;

import com.vgc.entity.*;
import com.vgc.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CategoryRepository categoryRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendancePhraseRepository attendancePhraseRepository;
    private final GachaPrizeRepository gachaPrizeRepository;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(CategoryRepository categoryRepository,
                           PartyRepository partyRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AttendancePhraseRepository attendancePhraseRepository,
                           GachaPrizeRepository gachaPrizeRepository) {
        this.categoryRepository = categoryRepository;
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.attendancePhraseRepository = attendancePhraseRepository;
        this.gachaPrizeRepository = gachaPrizeRepository;
    }

    @Override
    public void run(String... args) {
        initCategories();
        initParties();
        initAdminUser();
        initAttendancePhrases();
        initGachaPrizes();
    }

    private void initCategories() {
        createCategoryIfNotExists("긍정문구", "긍정 문구", "green", false);
        createCategoryIfNotExists("동료칭찬", "동료 칭찬", "blue", false);
        createCategoryIfNotExists("퀘스트", "퀘스트", "orange", true);
    }

    private void createCategoryIfNotExists(String name, String label, String color, boolean hasStatus) {
        if (!categoryRepository.existsByName(name)) {
            Category category = new Category();
            category.setName(name);
            category.setLabel(label);
            category.setColor(color);
            category.setHasStatus(hasStatus);
            categoryRepository.save(category);
        }
    }

    private void initParties() {
        String[] partyNames = {"TG1", "TG2", "TG3", "TG4", "TG5"};
        for (String name : partyNames) {
            if (!partyRepository.existsByName(name)) {
                Party party = new Party();
                party.setName(name);
                partyRepository.save(party);
            }
        }
    }

    private void initAdminUser() {
        String adminEmail = "admin@greenforest.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNickname("관리자");
            admin.setName("관리자");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }

    private void initAttendancePhrases() {
        if (attendancePhraseRepository.count() == 0) {
            attendancePhraseRepository.saveAll(List.of(
                phrase("오늘도 화이팅", "GENERAL"),
                phrase("좋은 아침입니다", "GENERAL"),
                phrase("출근 완료!", "GENERAL"),
                phrase("오늘도 무사히", "GENERAL"),
                phrase("커피 한 잔 하실래요?", "GENERAL"),
                phrase("월요병 탈출!", "MONDAY"),
                phrase("이번 주도 파이팅", "MONDAY"),
                phrase("주말이 코앞이에요", "FRIDAY"),
                phrase("불금입니다!", "FRIDAY"),
                phrase("한 주 수고하셨어요", "FRIDAY"),
                phrase("비 오는 날엔 커피죠", "RAINY"),
                phrase("우산 챙기셨어요?", "RAINY"),
                phrase("더운 하루, 물 많이 드세요", "HOT"),
                phrase("오늘은 추워요, 따뜻하게", "COLD")
            ));
        }
    }

    private void initGachaPrizes() {
        if (gachaPrizeRepository.count() == 0) {
            gachaPrizeRepository.saveAll(List.of(
                prize("스타벅스 아메리카노", 5000, 10, GachaPrizeTier.COMMON, new BigDecimal("1.00")),
                prize("GS25 5천원 상품권", 5000, 5, GachaPrizeTier.COMMON, new BigDecimal("1.00")),
                prize("배달의민족 1만원 상품권", 10000, 3, GachaPrizeTier.RARE, new BigDecimal("1.10")),
                prize("올리브영 2만원 상품권", 20000, 2, GachaPrizeTier.EPIC, new BigDecimal("1.20")),
                prize("프리미엄 와인 (2만원대)", 25000, 1, GachaPrizeTier.LEGENDARY, new BigDecimal("1.30"))
            ));
        }
    }

    private AttendancePhrase phrase(String text, String category) {
        AttendancePhrase p = new AttendancePhrase();
        p.setPhrase(text);
        p.setCategory(category);
        return p;
    }

    private GachaPrize prize(String name, int cashValue, int stock, GachaPrizeTier tier, BigDecimal ev) {
        GachaPrize p = new GachaPrize();
        p.setName(name);
        p.setCashValue(cashValue);
        p.setTotalStock(stock);
        p.setRemainingStock(stock);
        p.setTier(tier);
        p.setEvMultiplier(ev);
        p.setDisplayOrder(0);
        return p;
    }

}
