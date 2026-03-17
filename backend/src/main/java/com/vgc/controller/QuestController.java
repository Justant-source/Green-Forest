package com.vgc.controller;

import com.vgc.entity.Quest;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.QuestService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;
    private final UserRepository userRepository;

    public QuestController(QuestService questService, UserRepository userRepository) {
        this.questService = questService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getActiveQuests(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Quest> quests = questService.getActiveQuestsForUser(user);
        return quests.stream().map(q -> questToMap(q, user)).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getQuest(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Quest quest = questService.getQuest(id);
        return questToMap(quest, user);
    }

    @PostMapping("/{id}/vote")
    public Map<String, String> vote(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long votedForUserId = body.get("votedForUserId") != null
                ? ((Number) body.get("votedForUserId")).longValue() : null;
        String votedForOption = (String) body.get("votedForOption");

        questService.castVote(user, id, votedForUserId, votedForOption);
        return Map.of("status", "voted");
    }

    @GetMapping("/{id}/votes")
    public Map<String, Object> getVoteResults(@PathVariable Long id) {
        return questService.getVoteResults(id);
    }

    private Map<String, Object> questToMap(Quest quest, User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", quest.getId());
        map.put("title", quest.getTitle());
        map.put("description", quest.getDescription());
        map.put("rewardDrops", quest.getRewardDrops());
        map.put("startDate", quest.getStartDate().toString());
        map.put("endDate", quest.getEndDate().toString());
        map.put("targetType", quest.getTargetType());
        map.put("targetPartyId", quest.getTargetPartyId());
        map.put("maxCompletionsPerUser", quest.getMaxCompletionsPerUser());
        map.put("active", quest.isActive());
        map.put("voteType", quest.isVoteType());
        map.put("createdAt", quest.getCreatedAt().toString());
        map.put("completed", questService.isQuestCompletedByUser(quest.getId(), user.getId()));
        return map;
    }
}
