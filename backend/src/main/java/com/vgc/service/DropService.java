package com.vgc.service;

import com.vgc.entity.*;
import com.vgc.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.List;

@Service
public class DropService {

    private final DropTransactionRepository dropTransactionRepository;
    private final QuestCompletionLogRepository questCompletionLogRepository;
    private final QuestCompletionRepository questCompletionRepository;
    private final QuestRepository questRepository;
    private final PostTagRepository postTagRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PlantGrowthService plantGrowthService;
    private final ActivityLogService activityLogService;

    public DropService(DropTransactionRepository dropTransactionRepository,
                       QuestCompletionLogRepository questCompletionLogRepository,
                       QuestCompletionRepository questCompletionRepository,
                       QuestRepository questRepository,
                       PostTagRepository postTagRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       PlantGrowthService plantGrowthService,
                       ActivityLogService activityLogService) {
        this.dropTransactionRepository = dropTransactionRepository;
        this.questCompletionLogRepository = questCompletionLogRepository;
        this.questCompletionRepository = questCompletionRepository;
        this.questRepository = questRepository;
        this.postTagRepository = postTagRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.plantGrowthService = plantGrowthService;
        this.activityLogService = activityLogService;
    }

    /**
     * 게시글 작성 시 물방울 자동 지급 (핵심 로직)
     * @return 지급된 물방울 총량
     */
    @Transactional
    public int awardDropsForPost(User author, Post post, String category, List<User> taggedUsers) {
        int totalAwarded = 0;
        BigDecimal multiplier = author.getExpMultiplier() != null
                ? author.getExpMultiplier() : BigDecimal.ONE;

        switch (category) {
            case "긍정문구" -> {
                totalAwarded += awardDailyQuest(author, post, multiplier);
            }
            case "동료칭찬" -> {
                totalAwarded += awardWeeklyQuest(author, post, multiplier);
            }
            case "퀘스트" -> {
                if (post.getQuestId() != null) {
                    totalAwarded += awardQuestCompletion(author, post, multiplier);
                }
            }
        }

        // 태깅 보너스 처리
        if (taggedUsers != null && !taggedUsers.isEmpty()) {
            totalAwarded += awardTagBonuses(author, post, taggedUsers);
        }

        return totalAwarded;
    }

    /**
     * 긍정 문구 — 1일 1회, 10 물방울
     */
    private int awardDailyQuest(User author, Post post, BigDecimal multiplier) {
        String periodKey = LocalDate.now().toString(); // YYYY-MM-DD

        if (questCompletionLogRepository.existsByUserIdAndQuestTypeAndCategoryAndPeriodKey(
                author.getId(), "일일", "긍정문구", periodKey)) {
            return 0; // 이미 오늘 지급됨
        }

        int baseDrops = 10;
        int finalDrops = applyMultiplier(baseDrops, multiplier);

        // 중복 방지 로그 저장 (UNIQUE KEY로 동시성 방어)
        QuestCompletionLog log = new QuestCompletionLog();
        log.setUser(author);
        log.setQuestType("일일");
        log.setCategory("긍정문구");
        log.setPeriodKey(periodKey);
        log.setPostId(post.getId());
        questCompletionLogRepository.save(log);

        recordTransaction(author, finalDrops, DropReasonType.DAILY_QUEST,
                "긍정 문구 작성 보상", post.getId(), null);

        notificationService.createNotification(author, NotificationType.DROP_AWARD,
                "물방울 획득!", "긍정 문구 작성으로 💧" + finalDrops + " 물방울을 획득했어요!",
                post.getId(), null);

        return finalDrops;
    }

    /**
     * 동료 칭찬 — 주 1회, 30 물방울
     */
    private int awardWeeklyQuest(User author, Post post, BigDecimal multiplier) {
        LocalDate now = LocalDate.now();
        int weekNumber = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int weekYear = now.get(IsoFields.WEEK_BASED_YEAR);
        String periodKey = String.format("%d-W%02d", weekYear, weekNumber);

        if (questCompletionLogRepository.existsByUserIdAndQuestTypeAndCategoryAndPeriodKey(
                author.getId(), "주간", "동료칭찬", periodKey)) {
            return 0;
        }

        int baseDrops = 30;
        int finalDrops = applyMultiplier(baseDrops, multiplier);

        QuestCompletionLog log = new QuestCompletionLog();
        log.setUser(author);
        log.setQuestType("주간");
        log.setCategory("동료칭찬");
        log.setPeriodKey(periodKey);
        log.setPostId(post.getId());
        questCompletionLogRepository.save(log);

        recordTransaction(author, finalDrops, DropReasonType.WEEKLY_QUEST,
                "동료 칭찬 작성 보상", post.getId(), null);

        notificationService.createNotification(author, NotificationType.DROP_AWARD,
                "물방울 획득!", "동료 칭찬 작성으로 💧" + finalDrops + " 물방울을 획득했어요!",
                post.getId(), null);

        return finalDrops;
    }

    /**
     * 퀘스트 인증 — 퀘스트별 보상, max_completions_per_user 제한
     */
    private int awardQuestCompletion(User author, Post post, BigDecimal multiplier) {
        Quest quest = questRepository.findById(post.getQuestId())
                .orElseThrow(() -> new RuntimeException("퀘스트를 찾을 수 없습니다."));

        if (!quest.isActive()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(quest.getStartDate()) || today.isAfter(quest.getEndDate())) {
            return 0;
        }

        int completionCount = questCompletionRepository.countByQuestIdAndUserId(quest.getId(), author.getId());
        if (completionCount >= quest.getMaxCompletionsPerUser()) {
            return 0;
        }

        int finalDrops = applyMultiplier(quest.getRewardDrops(), multiplier);

        QuestCompletion completion = new QuestCompletion();
        completion.setQuest(quest);
        completion.setUser(author);
        completion.setPost(post);
        questCompletionRepository.save(completion);

        recordTransaction(author, finalDrops, DropReasonType.QUEST_COMPLETION,
                "퀘스트 '" + quest.getTitle() + "' 인증 보상", post.getId(), quest.getId());

        notificationService.createNotification(author, NotificationType.DROP_AWARD,
                "퀘스트 보상 획득!", "'" + quest.getTitle() + "' 퀘스트로 💧" + finalDrops + " 물방울을 획득했어요!",
                post.getId(), quest.getId());

        return finalDrops;
    }

    /**
     * 동료 칭찬 주 1회 여부 확인 (PostService에서 사전 검증용)
     */
    public boolean hasPraisedThisWeek(Long userId) {
        LocalDate now = LocalDate.now();
        String periodKey = String.format("%d-W%02d",
                now.get(IsoFields.WEEK_BASED_YEAR),
                now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        return questCompletionLogRepository.existsByUserIdAndQuestTypeAndCategoryAndPeriodKey(
                userId, "주간", "동료칭찬", periodKey);
    }

    /**
     * 태깅 보너스 — 1~2명: 각 30 물방울 / 3명 이상: 각 20 물방울 (동료칭찬 전용)
     */
    private int awardTagBonuses(User author, Post post, List<User> taggedUsers) {
        int totalTagBonus = 0;
        long validCount = taggedUsers.stream()
                .filter(u -> !u.getId().equals(author.getId()))
                .filter(u -> !postTagRepository.existsByPostIdAndTaggedUserId(post.getId(), u.getId()))
                .count();
        int tagDrops = validCount <= 2 ? 30 : 20;

        for (User taggedUser : taggedUsers) {
            if (taggedUser.getId().equals(author.getId())) {
                continue;
            }

            if (postTagRepository.existsByPostIdAndTaggedUserId(post.getId(), taggedUser.getId())) {
                continue;
            }

            PostTag postTag = new PostTag();
            postTag.setPost(post);
            postTag.setTaggedUser(taggedUser);
            postTagRepository.save(postTag);

            recordTransaction(taggedUser, tagDrops, DropReasonType.TAG_BONUS,
                    author.getNickname() + "님이 태깅|postId=" + post.getId(), post.getId(), null);

            notificationService.createNotification(taggedUser, NotificationType.TAG,
                    "태깅되었어요!",
                    (post.isAnonymous() ? "익명의 그린메이커" : author.getNickname())
                            + "님이 회원님을 태깅했어요! 💧" + tagDrops + " 물방울 획득!",
                    post.getId(), null);

            plantGrowthService.onPraiseReceived(taggedUser.getId(), post.getId());
            totalTagBonus += tagDrops;
        }

        return totalTagBonus;
    }

    /**
     * 태그 취소 — 태깅된 사람에게 준 TAG_BONUS를 회수하고 PostTag 삭제
     */
    @Transactional
    public void revokeTagBonus(User taggedUser, Post post) {
        List<DropTransaction> txList = dropTransactionRepository
                .findByUserIdAndReasonTypeAndRelatedPostId(taggedUser.getId(), DropReasonType.TAG_BONUS, post.getId());
        int totalRevoke = txList.stream().mapToInt(DropTransaction::getAmount).sum();
        if (totalRevoke > 0) {
            recordTransaction(taggedUser, -totalRevoke, DropReasonType.TAG_BONUS,
                    "태그 취소 환수|postId=" + post.getId(), post.getId(), null);
        }
        postTagRepository.findByPostId(post.getId()).stream()
                .filter(pt -> pt.getTaggedUser().getId().equals(taggedUser.getId()))
                .forEach(postTagRepository::delete);
        plantGrowthService.onPraiseRevoked(taggedUser.getId(), post.getId());
    }

    /**
     * 태그 추가 — 수정 시 새로 추가된 태그에 물방울 지급
     */
    @Transactional
    public void awardNewTagBonus(User author, Post post, User taggedUser) {
        if (taggedUser.getId().equals(author.getId())) return;
        if (postTagRepository.existsByPostIdAndTaggedUserId(post.getId(), taggedUser.getId())) return;

        PostTag postTag = new PostTag();
        postTag.setPost(post);
        postTag.setTaggedUser(taggedUser);
        postTagRepository.save(postTag);

        recordTransaction(taggedUser, 5, DropReasonType.TAG_BONUS,
                author.getNickname() + "님이 태깅|postId=" + post.getId(), post.getId(), null);

        notificationService.createNotification(taggedUser, NotificationType.TAG,
                "태깅되었어요!",
                (post.isAnonymous() ? "익명의 그린메이커" : author.getNickname())
                        + "님이 회원님을 태깅했어요! 💧5 물방울 획득!",
                post.getId(), null);

        plantGrowthService.onPraiseReceived(taggedUser.getId(), post.getId());
    }

    /**
     * 좋아요 보너스 — 좋아요 누른 사람에게 2 물방울 (글당 1회)
     */
    @Transactional
    public void awardDropsForLike(User liker, Post post) {
        if (dropTransactionRepository.existsByUserIdAndReasonTypeAndRelatedPostId(
                liker.getId(), DropReasonType.LIKE_BONUS, post.getId())) {
            return;
        }

        int drops = 2;
        recordTransaction(liker, drops, DropReasonType.LIKE_BONUS,
                "좋아요 보너스", post.getId(), null);

        notificationService.createNotification(liker, NotificationType.DROP_AWARD,
                "물방울 획득!", "좋아요를 눌러 💧" + drops + " 물방울을 획득했어요!",
                post.getId(), null);
    }

    /**
     * 댓글 보너스 — 댓글 작성자에게 5 물방울 (글당 최대 5회)
     */
    @Transactional
    public void awardDropsForComment(User commenter, Post post) {
        int count = dropTransactionRepository.countByUserIdAndReasonTypeAndRelatedPostId(
                commenter.getId(), DropReasonType.COMMENT_BONUS, post.getId());
        if (count >= 5) {
            return;
        }

        int drops = 5;
        recordTransaction(commenter, drops, DropReasonType.COMMENT_BONUS,
                "댓글 보너스", post.getId(), null);

        notificationService.createNotification(commenter, NotificationType.DROP_AWARD,
                "물방울 획득!", "댓글을 달아 💧" + drops + " 물방울을 획득했어요!",
                post.getId(), null);
    }

    /**
     * GM 수동 지급/차감
     */
    @Transactional
    public void gmManualAward(User admin, User target, int amount, String reason) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        DropReasonType reasonType = amount >= 0 ? DropReasonType.GM_AWARD : DropReasonType.GM_DEDUCT;
        recordTransaction(target, amount, reasonType, reason, null, null);

        String title = amount >= 0 ? "물방울 지급" : "물방울 차감";
        String body = "게임 마스터가 💧" + Math.abs(amount) + " 물방울을 "
                + (amount >= 0 ? "지급" : "차감") + "했어요. 사유: " + reason;
        notificationService.createNotification(target, NotificationType.DROP_AWARD,
                title, body, null, null);
    }

    /**
     * 물방울 선물 (유저 → 유저)
     */
    @Transactional
    public void giftDrops(User sender, User receiver, int amount) {
        if (amount <= 0) {
            throw new RuntimeException("선물할 물방울은 1 이상이어야 합니다.");
        }
        if (sender.getTotalDrops() < amount) {
            throw new RuntimeException("물방울이 부족합니다.");
        }

        recordTransaction(sender, -amount, DropReasonType.GIFT_SENT,
                receiver.getNickname() + "님에게 선물", null, null);
        recordTransaction(receiver, amount, DropReasonType.GIFT_RECEIVED,
                sender.getNickname() + "님이 선물", null, null);

        activityLogService.logDropGift(sender.getId(), sender.getNickname(), receiver.getId(), receiver.getNickname(), amount);

        notificationService.createNotification(receiver, NotificationType.DROP_AWARD,
                "물방울 선물 도착!",
                sender.getNickname() + "님이 💧" + amount + " 물방울을 선물했어요!",
                null, null);
    }

    /**
     * 물방울 거래 기록 + 잔고 갱신
     */
    private void recordTransaction(User user, int amount, DropReasonType reasonType,
                                   String detail, Long postId, Long questId) {
        DropTransaction tx = new DropTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setReasonType(reasonType);
        tx.setReasonDetail(detail);
        tx.setRelatedPostId(postId);
        tx.setRelatedQuestId(questId);
        dropTransactionRepository.save(tx);

        // 잔고 갱신
        user.setTotalDrops(user.getTotalDrops() + amount);
        // 누적 획득분 갱신 (양수 거래만)
        if (amount > 0) {
            user.setEarnedDrops(user.getEarnedDrops() + amount);
        }
        userRepository.save(user);
    }

    /**
     * 난이도 가중치 적용 (반올림하여 정수)
     */
    private int applyMultiplier(int baseDrops, BigDecimal multiplier) {
        return new BigDecimal(baseDrops)
                .multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    @Transactional
    public void awardAttendance(User user) {
        recordTransaction(user, 10, DropReasonType.ATTENDANCE, "출석 보상", null, null);
    }

    /**
     * 퀘스트 이벤트(사진 빙고 등) 보상 지급
     */
    @Transactional
    public void awardEventReward(User user, int amount, String memo) {
        if (amount <= 0) return;
        recordTransaction(user, amount, DropReasonType.EVENT_REWARD, memo, null, null);
    }

    @Transactional
    public void deductForGacha(User user, int amount, String prizeName, Long drawId, boolean isWinner) {
        String result = isWinner ? "당첨" : "꽝";
        recordTransaction(user, -amount, DropReasonType.GACHA_BET,
                "뽑기[" + prizeName + "] " + result + "|drawId=" + drawId, null, null);
    }
}
