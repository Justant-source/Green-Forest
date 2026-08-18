package com.vgc.service.event.photoexhibition;

import com.vgc.dto.event.photoexhibition.PhotoExhibitionPreviewResponse;
import com.vgc.entity.NotificationType;
import com.vgc.entity.Post;
import com.vgc.entity.PostImage;
import com.vgc.entity.PostStatus;
import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.EventType;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionImage;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionRewardGrant;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionSubmission;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionVote;
import com.vgc.repository.BookmarkRepository;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostLikeRepository;
import com.vgc.repository.PostRepository;
import com.vgc.repository.PostTagRepository;
import com.vgc.repository.QuestCompletionRepository;
import com.vgc.repository.event.EventRepository;
import com.vgc.repository.event.photoexhibition.PhotoExhibitionConfigRepository;
import com.vgc.repository.event.photoexhibition.PhotoExhibitionRewardGrantRepository;
import com.vgc.repository.event.photoexhibition.PhotoExhibitionSubmissionRepository;
import com.vgc.repository.event.photoexhibition.PhotoExhibitionVoteRepository;
import com.vgc.service.DropService;
import com.vgc.service.ImageStorageService;
import com.vgc.service.NotificationService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhotoExhibitionService {

    private final EventRepository events;
    private final PhotoExhibitionConfigRepository configs;
    private final PhotoExhibitionSubmissionRepository submissions;
    private final PhotoExhibitionVoteRepository votes;
    private final PhotoExhibitionRewardGrantRepository grants;
    private final ImageStorageService storage;
    private final PostRepository posts;
    private final BookmarkRepository bookmarks;
    private final PostLikeRepository postLikes;
    private final CommentRepository comments;
    private final PostTagRepository postTags;
    private final QuestCompletionRepository questCompletions;
    private final DropService drops;
    private final NotificationService notifications;

    public PhotoExhibitionService(
            EventRepository e,
            PhotoExhibitionConfigRepository c,
            PhotoExhibitionSubmissionRepository s,
            PhotoExhibitionVoteRepository v,
            PhotoExhibitionRewardGrantRepository g,
            ImageStorageService i,
            PostRepository p,
            BookmarkRepository b,
            PostLikeRepository l,
            CommentRepository cm,
            PostTagRepository pt,
            QuestCompletionRepository qc,
            DropService d,
            NotificationService n) {
        events = e;
        configs = c;
        submissions = s;
        votes = v;
        grants = g;
        storage = i;
        posts = p;
        bookmarks = b;
        postLikes = l;
        comments = cm;
        postTags = pt;
        questCompletions = qc;
        drops = d;
        notifications = n;
    }

    private Event event(Long id) {
        Event e = events.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (e.getType() != EventType.PHOTO_EXHIBITION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 전시회가 아닙니다.");
        }
        return e;
    }

    private void requireActive(Event event) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 이벤트가 아닙니다.");
        }
    }

    private PhotoExhibitionConfig config(Long id) {
        return configs.findByEventId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "전시회 설정이 없습니다."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    private boolean submissionOpen(PhotoExhibitionConfig c) {
        var n = now();
        return !n.isBefore(c.getSubmissionStart()) && n.isBefore(c.getSubmissionEnd());
    }

    private boolean votingOpen(PhotoExhibitionConfig c) {
        var n = now();
        return c.getVotingStartedAt() != null
                && !n.isBefore(c.getVotingStartedAt())
                && n.isBefore(c.getVotingEnd());
    }

    @Transactional
    public void startVoting(Long eventId) {
        Event e = event(eventId);
        requireActive(e);
        PhotoExhibitionConfig c = config(eventId);
        LocalDateTime n = now();
        if (n.isBefore(c.getSubmissionEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "출품 마감 후에만 투표를 시작할 수 있습니다.");
        }
        if (n.isBefore(c.getReviewEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "투표는 " + c.getReviewEnd() + " 이후부터 시작할 수 있습니다.");
        }
        if (c.getVotingStartedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 투표가 시작되었습니다.");
        }
        if (!n.isBefore(c.getVotingEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "투표 종료 시각이 지났습니다.");
        }
        c.setVotingStartedAt(n);
    }

    @Transactional
    public PhotoExhibitionSubmission mine(Long id, User u) {
        Event e = event(id);
        requireActive(e);
        return submissions.findByEventIdAndUserId(id, u.getId()).orElseGet(() -> {
            if (!submissionOpen(config(id))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간이 아닙니다.");
            }
            PhotoExhibitionSubmission s = new PhotoExhibitionSubmission();
            s.setEvent(e);
            s.setUser(u);
            return submissions.save(s);
        });
    }

    @Transactional
    public PhotoExhibitionSubmission update(Long id, User u, String title, String intro) {
        if (!submissionOpen(config(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간에만 수정할 수 있습니다.");
        }
        PhotoExhibitionSubmission s = mine(id, u);
        if (title == null || title.isBlank() || title.trim().length() > 150
                || intro == null || intro.isBlank() || intro.trim().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목(150자)과 소개(2000자)를 확인하세요.");
        }
        s.setTitle(title.trim());
        s.setIntroduction(intro.trim());
        sync(s, false);
        return s;
    }

    @Transactional
    public void deleteMySubmission(Long eventId, User user) {
        deleteSubmission(eventId, user, null);
    }

    @Transactional
    public void deleteSubmission(Long eventId, User actor, Long submissionId) {
        Event e = event(eventId);
        requireActive(e);
        if (!submissionOpen(config(eventId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간에만 삭제할 수 있습니다.");
        }
        boolean admin = "ADMIN".equals(actor.getRole());
        PhotoExhibitionSubmission s;
        if (submissionId != null) {
            s = submissions.findById(submissionId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "출품작이 없습니다."));
            if (!s.getEvent().getId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트 불일치");
            }
            if (!admin && !s.getUser().getId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 출품작만 삭제할 수 있습니다.");
            }
        } else {
            s = submissions.findByEventIdAndUserId(eventId, actor.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "출품작이 없습니다."));
        }
        if (s.getPlazaPostId() != null) {
            posts.findById(s.getPlazaPostId()).ifPresent(this::deleteGeneratedPost);
            s.setPlazaPostId(null);
        }
        votes.deleteBySubmissionId(s.getId());
        for (PhotoExhibitionImage image : List.copyOf(s.getImages())) {
            try {
                storage.delete(image.getImageUrl());
            } catch (Exception ignored) {
            }
        }
        s.getImages().clear();
        submissions.delete(s);
    }

    @Transactional
    public PhotoExhibitionSubmission upload(Long id, User u, MultipartFile f) {
        if (!submissionOpen(config(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간이 아닙니다.");
        }
        if (f == null || f.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지가 필요합니다.");
        }
        PhotoExhibitionSubmission s = mine(id, u);
        if (s.getImages().size() >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 4장입니다.");
        }
        try {
            s.getImages().add(new PhotoExhibitionImage(s, storage.upload(f), s.getImages().size()));
        } catch (IOException x) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 실패", x);
        }
        sync(s, false);
        return s;
    }

    @Transactional
    public PhotoExhibitionSubmission deleteImage(Long id, User u, Long imageId) {
        Event e = event(id);
        requireActive(e);
        if (!submissionOpen(config(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간이 아닙니다.");
        }
        PhotoExhibitionSubmission s = mine(id, u);
        PhotoExhibitionImage image = s.getImages().stream()
                .filter(x -> x.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."));
        String url = image.getImageUrl();
        for (int i = 0; i < s.getImages().size(); i++) {
            s.getImages().get(i).setSortOrder(1000 + i);
        }
        submissions.flush();
        s.getImages().remove(image);
        normalize(s);
        try {
            storage.delete(url);
        } catch (Exception ignored) {
        }
        sync(s, false);
        return s;
    }

    @Transactional
    public PhotoExhibitionSubmission reorderImages(Long id, User u, List<Long> imageIds) {
        Event e = event(id);
        requireActive(e);
        if (!submissionOpen(config(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 기간이 아닙니다.");
        }
        PhotoExhibitionSubmission s = mine(id, u);
        if (imageIds == null || imageIds.size() != s.getImages().size()
                || new HashSet<>(imageIds).size() != imageIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 이미지 전체 순서가 필요합니다.");
        }
        Map<Long, PhotoExhibitionImage> byId = s.getImages().stream()
                .collect(Collectors.toMap(PhotoExhibitionImage::getId, x -> x));
        for (Long imageId : imageIds) {
            if (!byId.containsKey(imageId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 작품의 이미지는 사용할 수 없습니다.");
            }
        }
        for (int i = 0; i < s.getImages().size(); i++) {
            s.getImages().get(i).setSortOrder(1000 + i);
        }
        submissions.flush();
        for (int i = 0; i < imageIds.size(); i++) {
            byId.get(imageIds.get(i)).setSortOrder(i);
        }
        s.getImages().sort(Comparator.comparingInt(PhotoExhibitionImage::getSortOrder));
        sync(s, false);
        return s;
    }

    private void normalize(PhotoExhibitionSubmission s) {
        s.getImages().sort(Comparator.comparingInt(PhotoExhibitionImage::getSortOrder));
        for (int i = 0; i < s.getImages().size(); i++) {
            s.getImages().get(i).setSortOrder(i);
        }
    }

    @Transactional
    public List<Long> vote(Long id, User voter, List<Long> ids) {
        Event activeEvent = event(id);
        requireActive(activeEvent);
        PhotoExhibitionConfig c = config(id);
        if (!votingOpen(c)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "투표 기간이 아닙니다.");
        }
        List<Long> safe = ids == null ? List.of() : ids;
        if (safe.size() > 3 || new HashSet<>(safe).size() != safe.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택은 서로 다른 0~3개여야 합니다.");
        }
        // Locking the event serializes a voter's delete-and-replace ballot with finalization.
        Event locked = events.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        requireActive(locked);
        List<PhotoExhibitionSubmission> selected = safe.stream()
                .map(x -> submissions.findById(x).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "작품을 찾을 수 없습니다.")))
                .toList();
        for (var s : selected) {
            if (!s.getEvent().getId().equals(id) || !s.isValid() || s.getUser().getId().equals(voter.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "투표할 수 없는 작품입니다.");
            }
        }
        votes.deleteByEventIdAndVoterId(id, voter.getId());
        votes.flush();
        for (var s : selected) {
            votes.save(new PhotoExhibitionVote(locked, voter, s));
        }
        grantVoterReward(locked, voter, safe.size());
        return safe;
    }

    @Transactional
    public PhotoExhibitionSubmission exclude(Long id, Long subId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제외 사유는 필수입니다.");
        }
        Event e = event(id);
        if (e.getStatus() == EventStatus.SCORED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "확정된 이벤트는 제외할 수 없습니다.");
        }
        PhotoExhibitionSubmission s = submissions.findById(subId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "작품을 찾을 수 없습니다."));
        if (!s.getEvent().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트 불일치");
        }
        s.setExcluded(true);
        s.setExclusionReason(reason);
        votes.deleteBySubmissionId(s.getId());
        votes.flush();
        sync(s, false);
        notifications.createNotification(
                s.getUser(), NotificationType.EVENT_REWARD, "전시회 출품 제외", reason, null, null);
        return s;
    }

    @Transactional(readOnly = true)
    public EventStatus status(Long id) {
        return event(id).getStatus();
    }

    @Transactional
    public List<PhotoExhibitionSubmission> gallery(Long id, boolean result, Long viewerId) {
        Event e = event(id);
        if (e.getStatus() == EventStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공개 전 이벤트입니다.");
        }
        PhotoExhibitionConfig c = config(id);
        PhotoExhibitionPhase phase = PhotoExhibitionPhaseResolver.resolve(c, e.getStatus(), now());
        if (phase == PhotoExhibitionPhase.SCHEDULED
                || phase == PhotoExhibitionPhase.REVIEW
                || phase == PhotoExhibitionPhase.TALLY_PENDING
                || phase == PhotoExhibitionPhase.RESULT) {
            return List.of();
        }
        var list = submissions.findByEventIdOrderByCreatedAtAsc(id).stream()
                .filter(s -> !s.isExcluded() && s.isValid())
                .collect(Collectors.toList());
        if (phase == PhotoExhibitionPhase.VOTING && !result) {
            Collections.shuffle(list, new Random(31L * id + (viewerId == null ? 0 : viewerId)));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<PhotoExhibitionSubmission> adminList(Long id) {
        event(id);
        return submissions.findByEventIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<PhotoExhibitionVote> auditVotes(Long id) {
        event(id);
        return votes.findByEventId(id);
    }

    @Transactional(readOnly = true)
    public List<Long> myVoteIds(Long eventId, User user) {
        event(eventId);
        return votes.findByEventIdAndVoterId(eventId, user.getId()).stream()
                .map(v -> v.getSubmission().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public PhotoExhibitionPreviewResponse preview(Long eventId) {
        event(eventId);
        List<PhotoExhibitionSubmission> all = submissions.findByEventIdOrderByCreatedAtAsc(eventId);
        List<PhotoExhibitionVote> ballot = votes.findByEventId(eventId);
        Map<Long, Integer> counts = new HashMap<>();
        for (var v : ballot) {
            counts.merge(v.getSubmission().getId(), 1, Integer::sum);
        }
        var ranking = PhotoExhibitionRankingCalculator.calculate(all.stream()
                .filter(PhotoExhibitionSubmission::isValid)
                .map(s -> new PhotoExhibitionRankingCalculator.Candidate(
                        s.getId(), counts.getOrDefault(s.getId(), 0)))
                .toList());
        Map<Long, PhotoExhibitionRankingCalculator.Recipient> recipients = ranking.recipients().stream()
                .collect(Collectors.toMap(PhotoExhibitionRankingCalculator.Recipient::submissionId, x -> x));
        List<PhotoExhibitionPreviewResponse.Candidate> candidates = all.stream()
                .filter(PhotoExhibitionSubmission::isValid)
                .map(s -> {
                    var r = recipients.get(s.getId());
                    return new PhotoExhibitionPreviewResponse.Candidate(
                            s.getId(),
                            s.getUser().getNickname(),
                            s.getTitle(),
                            counts.getOrDefault(s.getId(), 0),
                            r == null ? null : r.tier(),
                            r == null ? 0 : r.drops());
                })
                .toList();
        int participant = (int) all.stream().filter(PhotoExhibitionSubmission::isValid).count();
        int unique = (int) ballot.stream().map(v -> v.getVoter().getId()).distinct().count();
        return new PhotoExhibitionPreviewResponse(
                participant,
                unique,
                ballot.size(),
                participant * 100,
                ballot.size() * 10,
                ranking.rankDrops(),
                candidates);
    }

    @Transactional
    public void finalizeResults(Long id) {
        PhotoExhibitionConfig c = config(id);
        if (now().isBefore(c.getVotingEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "투표 종료 후 집계할 수 있습니다.");
        }
        Event e = events.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (e.getStatus() == EventStatus.SCORED) {
            return;
        }
        List<PhotoExhibitionSubmission> all = submissions.findByEventIdOrderByCreatedAtAsc(id);
        for (var s : all) {
            s.setFinalVotes((int) votes.countByEventIdAndSubmissionId(id, s.getId()));
            if (s.isValid()) {
                grant(e, s.getUser(), "PARTICIPANT", 100, e.getTitle() + " 참가 보상");
            }
        }
        Map<Long, PhotoExhibitionSubmission> byId = all.stream()
                .collect(Collectors.toMap(PhotoExhibitionSubmission::getId, s -> s));
        var result = PhotoExhibitionRankingCalculator.calculate(all.stream()
                .filter(PhotoExhibitionSubmission::isValid)
                .map(s -> new PhotoExhibitionRankingCalculator.Candidate(s.getId(), s.getFinalVotes()))
                .toList());
        for (var recipient : result.recipients()) {
            var s = byId.get(recipient.submissionId());
            s.setResultTier(recipient.tier());
            grant(e, s.getUser(), recipient.tier(), recipient.drops(), e.getTitle() + " " + recipient.tier() + " 수상");
            sync(s, true);
        }
        // voter reward is based on final saved selection, once only as event switches SCORED
        Set<Long> voterIds = new HashSet<>();
        for (var vote : votes.findByEventId(id)) {
            voterIds.add(vote.getVoter().getId());
        }
        for (Long voterId : voterIds) {
            int count = votes.findByEventIdAndVoterId(id, voterId).size();
            var user = votes.findByEventIdAndVoterId(id, voterId).get(0).getVoter();
            grant(e, user, "VOTER", count * 10, e.getTitle() + " 투표 보상");
        }
        for (var s : all) {
            if (s.isValid()) {
                sync(s, true);
            }
        }
        e.setStatus(EventStatus.SCORED);
    }

    /** 출품 100💧와 현재 투표 10/20/30💧를 지금 지급한다. 중복 지급은 grant 유니크로 막는다. */
    @Transactional
    public void payoutLiveRewards(Long id) {
        Event e = event(id);
        requireActive(e);
        for (var s : submissions.findByEventIdOrderByCreatedAtAsc(id)) {
            if (s.isValid()) {
                grant(e, s.getUser(), "PARTICIPANT", 100, e.getTitle() + " 참가 보상");
            }
        }
        Set<Long> voterIds = new HashSet<>();
        for (var vote : votes.findByEventId(id)) {
            voterIds.add(vote.getVoter().getId());
        }
        for (Long voterId : voterIds) {
            var ballot = votes.findByEventIdAndVoterId(id, voterId);
            grantVoterReward(e, ballot.get(0).getVoter(), ballot.size());
        }
    }

    private void grantVoterReward(Event event, User user, int count) {
        int amount = count * 10;
        if (amount <= 0) {
            return;
        }
        var existing = grants.findByEventIdAndUserIdAndGrantKind(event.getId(), user.getId(), "VOTER");
        if (existing.isEmpty()) {
            grant(event, user, "VOTER", amount, event.getTitle() + " 투표 보상");
            return;
        }
        PhotoExhibitionRewardGrant row = existing.get();
        int extra = amount - row.getAmount();
        if (extra <= 0) {
            return;
        }
        row.setAmount(amount);
        drops.awardEventReward(user, extra, event.getTitle() + " 투표 보상 추가");
        notifications.createNotification(
                user,
                NotificationType.EVENT_REWARD,
                "전시회 보상",
                event.getTitle() + " 투표 보상 추가 — 💧" + extra,
                null,
                null);
    }

    private void grant(Event event, User user, String kind, int amount, String memo) {
        if (grants.existsByEventIdAndUserIdAndGrantKind(event.getId(), user.getId(), kind)) {
            return;
        }
        grants.save(new PhotoExhibitionRewardGrant(event, user, kind, amount));
        if (amount > 0) {
            drops.awardEventReward(user, amount, memo);
        }
        notifications.createNotification(
                user,
                NotificationType.EVENT_REWARD,
                "전시회 결과",
                memo + (amount > 0 ? " — 💧" + amount : ""),
                null,
                null);
    }

    private void sync(PhotoExhibitionSubmission s, boolean reveal) {
        if (!s.isValid()) {
            if (s.getPlazaPostId() != null) {
                posts.findById(s.getPlazaPostId()).ifPresent(this::deleteGeneratedPost);
                s.setPlazaPostId(null);
            }
            return;
        }
        Post p = s.getPlazaPostId() == null
                ? new Post()
                : posts.findById(s.getPlazaPostId()).orElseGet(Post::new);
        p.setTitle(s.getTitle());
        p.setContent(s.getIntroduction());
        p.setCategory("이벤트");
        p.setAuthor(s.getUser());
        p.setAnonymous(!reveal);
        p.setStatus(PostStatus.REGISTERED);
        p.setPhotoExhibitionSubmissionId(s.getId());
        p.setPhotoExhibitionEventId(s.getEvent().getId());
        p.setImageUrl(s.getImages().get(0).getImageUrl());
        p.getImages().clear();
        for (int i = 0; i < s.getImages().size(); i++) {
            p.getImages().add(new PostImage(p, s.getImages().get(i).getImageUrl(), i));
        }
        p = posts.save(p);
        s.setPlazaPostId(p.getId());
    }

    private void deleteGeneratedPost(Post post) {
        Long id = post.getId();
        questCompletions.deleteByPostId(id);
        postTags.deleteByPostId(id);
        bookmarks.deleteByPostId(id);
        postLikes.deleteByPostId(id);
        comments.deleteByPostId(id);
        posts.delete(post);
    }
}
