package com.vgc.config;

import com.vgc.entity.Category;
import com.vgc.entity.Party;
import com.vgc.entity.User;
import com.vgc.repository.CategoryRepository;
import com.vgc.repository.PartyRepository;
import com.vgc.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CategoryRepository categoryRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           PartyRepository partyRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initCategories();
        initParties();
        initAdminUser();
    }

    private void initCategories() {
        createCategoryIfNotExists("긍정문구", "긍정 문구", "green", false);
        createCategoryIfNotExists("동료칭찬", "동료 칭찬", "blue", false);
        createCategoryIfNotExists("퀘스트", "퀘스트", "orange", false);
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
        String adminEmail = "gm@gm.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("greenforest2026!"));
            admin.setNickname("게임 마스터");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }
}
