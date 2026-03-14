package com.vgc.service;

import com.vgc.entity.*;
import com.vgc.repository.QuestCompletionRepository;
import com.vgc.repository.QuestRepository;
import com.vgc.repository.UserRepository;
import com.vgc.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final QuestCompletionRepository questCompletionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public QuestService(QuestRepository questRepository,
                        QuestCompletionRepository questCompletionRepository,
                        VoteRepository voteRepository,
                        UserRepository userRepository,
                        NotificationService notificationService) {
        this.questRepository = questRepository;
        this.questCompletionRepository = questCompletionRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<Quest> getActiveQuests() {
        return questRepository.findActiveQuestsForDate(LocalDate.now());
    }

    public List<Quest> getActiveQuestsForUser(User user) {
        Long partyId = user.getParty() != null ? user.getParty().getId() : null;
        if (partyId != null) {
            return questRepository.findActiveQuestsForUserParty(LocalDate.now(), partyId);
        }
        return questRepository.findActiveQuestsForDate(LocalDate.now());
    }

    public Quest getQuest(Long id) {
        return questRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("퀘스트를 찾을 수 없습니다."));
    }

    @Transactional
    public Quest createQuest(User admin, String title, String description, int rewardDrops,
                             LocalDate startDate, LocalDate endDate, String targetType,
                             Long targetPartyId, int maxCompletionsPerUser, boolean isVoteType) {
        Quest quest = new Quest();
        quest.setTitle(title);
        quest.setDescription(description);
        quest.setRewardDrops(rewardDrops);
        quest.setStartDate(startDate);
        quest.setEndDate(endDate);
        quest.setTargetType(targetType);
        quest.setTargetPartyId(targetPartyId);
        quest.setMaxCompletionsPerUser(maxCompletionsPerUser);
        quest.setVoteType(isVoteType);
        quest.setCreatedBy(admin);
        Quest saved = questRepository.save(quest);

        // 모든 유저에게 알림
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getId().equals(admin.getId())) continue;
            notificationService.createNotification(user, NotificationType.QUEST_CREATED,
                    "새로운 퀘스트!",
                    "'" + title + "' 퀘스트가 시작되었어요! 보상: 💧" + rewardDrops,
                    null, saved.getId());
        }

        return saved;
    }

    @Transactional
    public Quest updateQuest(Long id, String title, String description, int rewardDrops,
                             LocalDate startDate, LocalDate endDate, String targetType,
                             Long targetPartyId, int maxCompletionsPerUser, boolean isActive) {
        Quest quest = questRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("퀘스트를 찾을 수 없습니다."));
        quest.setTitle(title);
        quest.setDescription(description);
        quest.setRewardDrops(rewardDrops);
        quest.setStartDate(startDate);
        quest.setEndDate(endDate);
        quest.setTargetType(targetType);
        quest.setTargetPartyId(targetPartyId);
        quest.setMaxCompletionsPerUser(maxCompletionsPerUser);
        quest.setActive(isActive);
        return questRepository.save(quest);
    }

    @Transactional
    public void deleteQuest(Long id) {
        questRepository.deleteById(id);
    }

    /**
     * 투표
     */
    @Transactional
    public void castVote(User voter, Long questId, Long votedForUserId, String votedForOption) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("퀘스트를 찾을 수 없습니다."));

        if (!quest.isVoteType()) {
            throw new RuntimeException("투표형 퀘스트가 아닙니다.");
        }

        if (voteRepository.existsByQuestIdAndUserId(questId, voter.getId())) {
            throw new RuntimeException("이미 투표했습니다.");
        }

        Vote vote = new Vote();
        vote.setQuest(quest);
        vote.setUser(voter);
        vote.setVotedForOption(votedForOption);

        if (votedForUserId != null) {
            User votedForUser = userRepository.findById(votedForUserId)
                    .orElseThrow(() -> new RuntimeException("투표 대상을 찾을 수 없습니다."));
            vote.setVotedForUser(votedForUser);
        }

        voteRepository.save(vote);
    }

    /**
     * 투표 결과 조회
     */
    public Map<String, Object> getVoteResults(Long questId) {
        Map<String, Object> results = new LinkedHashMap<>();

        List<Object[]> optionResults = voteRepository.countByQuestIdGroupByOption(questId);
        List<Map<String, Object>> options = new ArrayList<>();
        for (Object[] row : optionResults) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("option", row[0]);
            entry.put("count", ((Number) row[1]).longValue());
            options.add(entry);
        }
        results.put("optionVotes", options);

        List<Object[]> userResults = voteRepository.countByQuestIdGroupByUser(questId);
        List<Map<String, Object>> userVotes = new ArrayList<>();
        for (Object[] row : userResults) {
            Map<String, Object> entry = new LinkedHashMap<>();
            Long userId = (Long) row[0];
            entry.put("userId", userId);
            userRepository.findById(userId).ifPresent(u -> entry.put("nickname", u.getNickname()));
            entry.put("count", ((Number) row[1]).longValue());
            userVotes.add(entry);
        }
        results.put("userVotes", userVotes);

        return results;
    }

    /**
     * 유저의 퀘스트 완료 여부
     */
    public boolean isQuestCompletedByUser(Long questId, Long userId) {
        return questCompletionRepository.existsByQuestIdAndUserId(questId, userId);
    }
}
