package com.vgc.service;

import com.vgc.entity.PlantGrowth;
import com.vgc.entity.User;
import com.vgc.repository.PlantGrowthRepository;
import com.vgc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PlantGrowthService {

    private final PlantGrowthRepository plantGrowthRepository;
    private final UserRepository userRepository;

    public PlantGrowthService(PlantGrowthRepository plantGrowthRepository, UserRepository userRepository) {
        this.plantGrowthRepository = plantGrowthRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void onLikeReceived(Long authorId) {
        PlantGrowth growth = getOrCreate(authorId);
        growth.setLikesReceived(growth.getLikesReceived() + 1);
        recalculate(growth);
        plantGrowthRepository.save(growth);
    }

    @Transactional
    public void onCommentReceived(Long authorId) {
        PlantGrowth growth = getOrCreate(authorId);
        growth.setCommentsReceived(growth.getCommentsReceived() + 1);
        recalculate(growth);
        plantGrowthRepository.save(growth);
    }

    @Transactional
    public void onPraiseReceived(Long taggedUserId) {
        PlantGrowth growth = getOrCreate(taggedUserId);
        growth.setPraisesReceived(growth.getPraisesReceived() + 1);
        recalculate(growth);
        plantGrowthRepository.save(growth);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyGrowth(Long userId) {
        PlantGrowth growth = plantGrowthRepository.findByUserId(userId)
                .orElse(defaultGrowth(userId));
        return toResponse(growth);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserGrowth(Long userId) {
        PlantGrowth growth = plantGrowthRepository.findByUserId(userId)
                .orElse(defaultGrowth(userId));
        return toPublicResponse(growth);
    }

    private PlantGrowth getOrCreate(Long userId) {
        return plantGrowthRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            PlantGrowth g = new PlantGrowth();
            g.setUser(user);
            return g;
        });
    }

    private void recalculate(PlantGrowth growth) {
        int score = growth.getLikesReceived() * 1
                  + growth.getCommentsReceived() * 3
                  + growth.getPraisesReceived() * 10;
        growth.setGrowthScore(score);
        int newStage = calculateStage(score);
        if (newStage > growth.getStage()) {
            growth.setStage(newStage);
            growth.setLastGrownAt(LocalDateTime.now());
        }
    }

    private int calculateStage(int score) {
        if (score >= 300) return 5;
        if (score >= 150) return 4;
        if (score >= 70)  return 3;
        if (score >= 30)  return 2;
        if (score >= 10)  return 1;
        return 0;
    }

    private PlantGrowth defaultGrowth(Long userId) {
        PlantGrowth g = new PlantGrowth();
        return g;
    }

    private int nextStageScore(int stage) {
        int[] thresholds = {10, 30, 70, 150, 300};
        if (stage >= 5) return -1;
        return thresholds[stage];
    }

    private String stageLabel(int stage) {
        String[] labels = {"씨앗", "새싹", "잎2장", "꽃봉오리", "꽃", "열매"};
        return stage < labels.length ? labels[stage] : "열매";
    }

    private Map<String, Object> toResponse(PlantGrowth growth) {
        Map<String, Object> map = new HashMap<>();
        map.put("stage", growth.getStage());
        map.put("stageLabel", stageLabel(growth.getStage()));
        map.put("score", growth.getGrowthScore());
        map.put("likesReceived", growth.getLikesReceived());
        map.put("commentsReceived", growth.getCommentsReceived());
        map.put("praisesReceived", growth.getPraisesReceived());
        map.put("nextStageScore", nextStageScore(growth.getStage()));
        map.put("lastGrownAt", growth.getLastGrownAt() != null ? growth.getLastGrownAt().toString() : null);
        return map;
    }

    private Map<String, Object> toPublicResponse(PlantGrowth growth) {
        return Map.of(
            "stage", growth.getStage(),
            "stageLabel", stageLabel(growth.getStage()),
            "score", growth.getGrowthScore()
        );
    }
}
