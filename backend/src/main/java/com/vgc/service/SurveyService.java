package com.vgc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgc.dto.SurveyCreateRequest;
import com.vgc.dto.SurveyOptionInput;
import com.vgc.entity.*;
import com.vgc.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyService {

    private static final int MAX_ADMIN_OPTIONS = 15;
    private static final int MAX_TOTAL_OPTIONS = 30;
    private static final int MAX_USER_OPTIONS_PER_USER = 3;
    private static final int MAX_TEXT_LENGTH = 50;
    private static final int MAX_THUMBNAIL_IMAGES = 9;

    private final SurveyRepository surveyRepository;
    private final SurveyOptionRepository optionRepository;
    private final SurveyVoteRepository voteRepository;
    private final ImageStorageService imageStorageService;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;

    public SurveyService(SurveyRepository surveyRepository,
                         SurveyOptionRepository optionRepository,
                         SurveyVoteRepository voteRepository,
                         ImageStorageService imageStorageService,
                         PostRepository postRepository,
                         PostImageRepository postImageRepository) {
        this.surveyRepository = surveyRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.imageStorageService = imageStorageService;
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
    }

    /** 설문 + 게시글을 한 번에 생성 (관리자 전용). */
    @Transactional
    public Survey createSurveyWithPost(User admin, String title,
                                       SurveyCreateRequest req,
                                       List<SurveyOptionInput> inputs) throws IOException {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 설문을 생성할 수 있습니다.");

        Post post = new Post();
        post.setTitle(title);
        post.setContent("");
        post.setCategory("survey");
        post.setAuthor(admin);
        post.setAnonymous(false);
        postRepository.save(post);

        return createSurvey(post, req, inputs);
    }

    @Transactional
    public Survey createSurvey(Post post, SurveyCreateRequest req,
                               List<SurveyOptionInput> inputs) throws IOException {
        if (inputs == null || inputs.size() < 2)
            throw new IllegalArgumentException("옵션은 최소 2개 필요합니다.");
        if (inputs.size() > MAX_ADMIN_OPTIONS)
            throw new IllegalArgumentException("옵션은 최대 " + MAX_ADMIN_OPTIONS + "개까지 가능합니다.");
        if (req.getClosesAt() == null || req.getClosesAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("종료일은 현재 시각 이후여야 합니다.");

        Survey survey = new Survey();
        survey.setPost(post);
        survey.setClosesAt(req.getClosesAt());
        survey.setAnonymous(req.isAnonymous());
        survey.setAllowOptionAddByUser(req.isAllowOptionAddByUser());
        survey.setAllowMultiSelect(req.isAllowMultiSelect());
        survey.setNotice(req.isNotice());
        surveyRepository.save(survey);

        int order = 0;
        int imageOrder = 0;
        String firstImageUrl = null;

        for (SurveyOptionInput in : inputs) {
            validateOptionInput(in);
            SurveyOption opt = new SurveyOption();
            opt.setSurvey(survey);
            opt.setOptionType(in.getType());
            opt.setDisplayOrder(order++);

            if (in.getType() != SurveyOptionType.IMAGE_ONLY) {
                opt.setTextContent(in.getText());
            }
            if (in.getType() != SurveyOptionType.TEXT_ONLY && in.getImage() != null && !in.getImage().isEmpty()) {
                String url = imageStorageService.upload(in.getImage());
                opt.setImageUrl(url);
                // 피드 썸네일용으로 PostImage에도 저장 (최대 9장)
                if (imageOrder < MAX_THUMBNAIL_IMAGES) {
                    PostImage pi = new PostImage(post, url, imageOrder);
                    post.getImages().add(pi);
                    if (imageOrder == 0) {
                        firstImageUrl = url;
                        post.setImageUrl(url);
                    }
                    imageOrder++;
                }
            }
            optionRepository.save(opt);
        }

        if (firstImageUrl != null) {
            postRepository.save(post);
        }

        return survey;
    }

    private void validateOptionInput(SurveyOptionInput in) {
        switch (in.getType()) {
            case TEXT_ONLY -> {
                if (in.getText() == null || in.getText().isBlank())
                    throw new IllegalArgumentException("텍스트 옵션의 내용은 비울 수 없습니다.");
                if (in.getText().length() > MAX_TEXT_LENGTH)
                    throw new IllegalArgumentException("옵션 텍스트는 " + MAX_TEXT_LENGTH + "자 이하여야 합니다.");
            }
            case IMAGE_ONLY -> {
                if (in.getImage() == null || in.getImage().isEmpty())
                    throw new IllegalArgumentException("이미지 옵션은 이미지가 필요합니다.");
            }
            case TEXT_AND_IMAGE -> {
                if (in.getText() == null || in.getText().isBlank())
                    throw new IllegalArgumentException("텍스트+이미지 옵션의 텍스트는 비울 수 없습니다.");
                if (in.getText().length() > MAX_TEXT_LENGTH)
                    throw new IllegalArgumentException("옵션 텍스트는 " + MAX_TEXT_LENGTH + "자 이하여야 합니다.");
                if (in.getImage() == null || in.getImage().isEmpty())
                    throw new IllegalArgumentException("텍스트+이미지 옵션은 이미지가 필요합니다.");
            }
        }
    }

    /** 사용자가 텍스트 옵션 추가 (allowOptionAddByUser == true 인 설문만). */
    @Transactional
    public SurveyOption addUserOption(Long surveyId, User user, String text) {
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));
        if (!survey.isAllowOptionAddByUser())
            throw new IllegalArgumentException("이 설문은 참여자 옵션 추가가 허용되지 않습니다.");
        if (LocalDateTime.now().isAfter(survey.getClosesAt()))
            throw new IllegalArgumentException("종료된 설문입니다.");
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("옵션 텍스트는 비울 수 없습니다.");
        if (text.length() > MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("옵션 텍스트는 " + MAX_TEXT_LENGTH + "자 이하여야 합니다.");

        long total = optionRepository.countBySurveyId(surveyId);
        if (total >= MAX_TOTAL_OPTIONS)
            throw new IllegalArgumentException("옵션이 최대치(" + MAX_TOTAL_OPTIONS + "개)에 도달했습니다.");

        long byUser = optionRepository.countBySurveyIdAndAddedByUserId(surveyId, user.getId());
        if (byUser >= MAX_USER_OPTIONS_PER_USER)
            throw new IllegalArgumentException("1인당 최대 " + MAX_USER_OPTIONS_PER_USER + "개까지 추가 가능합니다.");

        SurveyOption opt = new SurveyOption();
        opt.setSurvey(survey);
        opt.setOptionType(SurveyOptionType.TEXT_ONLY);
        opt.setTextContent(text.trim());
        opt.setDisplayOrder((int) total);
        opt.setAddedByUser(user);
        return optionRepository.save(opt);
    }

    /** 투표. allowMultiSelect 에 따라 동작 다름. 같은 옵션 다시 누르면 토글 해제. */
    @Transactional
    public void vote(Long surveyId, Long optionId, User user) {
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));
        if (LocalDateTime.now().isAfter(survey.getClosesAt()))
            throw new IllegalArgumentException("종료된 설문입니다.");

        SurveyOption option = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다."));
        if (!option.getSurvey().getId().equals(surveyId))
            throw new IllegalArgumentException("옵션이 이 설문에 속하지 않습니다.");

        boolean alreadyVotedThisOption = voteRepository.existsByUserIdAndOptionId(user.getId(), optionId);
        if (alreadyVotedThisOption) {
            voteRepository.deleteByUserIdAndOptionId(user.getId(), optionId);
            return;
        }

        if (!survey.isAllowMultiSelect()) {
            List<SurveyVote> existing = voteRepository.findBySurveyIdAndUserId(surveyId, user.getId());
            voteRepository.deleteAll(existing);
        }

        SurveyVote vote = new SurveyVote();
        vote.setSurvey(survey);
        vote.setOption(option);
        vote.setUser(user);
        voteRepository.save(vote);
    }

    /** 설문 상세 조회 (옵션별 투표 수 포함). */
    @Transactional(readOnly = true)
    public Map<String, Object> getSurveyDetail(Long postId, User currentUser) {
        Survey survey = surveyRepository.findByPostId(postId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        Map<Long, Long> countByOption = new HashMap<>();
        for (Object[] row : voteRepository.countBySurveyGroupByOption(survey.getId())) {
            countByOption.put((Long) row[0], (Long) row[1]);
        }

        Set<Long> myVotedOptionIds = currentUser != null
            ? voteRepository.findBySurveyIdAndUserId(survey.getId(), currentUser.getId())
                .stream().map(v -> v.getOption().getId()).collect(Collectors.toSet())
            : Collections.emptySet();

        long totalVotes = countByOption.values().stream().mapToLong(Long::longValue).sum();

        List<Map<String, Object>> options = optionRepository
            .findBySurveyIdOrderByDisplayOrderAscIdAsc(survey.getId())
            .stream().map(o -> {
                long count = countByOption.getOrDefault(o.getId(), 0L);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", o.getId());
                m.put("type", o.getOptionType().name());
                m.put("text", o.getTextContent());
                m.put("imageUrl", o.getImageUrl());
                m.put("voteCount", count);
                m.put("voted", myVotedOptionIds.contains(o.getId()));
                m.put("addedByUser", o.getAddedByUser() != null);
                return m;
            }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", survey.getId());
        result.put("postId", survey.getPost().getId());
        result.put("closesAt", survey.getClosesAt().toString());
        result.put("anonymous", survey.isAnonymous());
        result.put("allowOptionAddByUser", survey.isAllowOptionAddByUser());
        result.put("allowMultiSelect", survey.isAllowMultiSelect());
        result.put("notice", survey.isNotice());
        result.put("closed", LocalDateTime.now().isAfter(survey.getClosesAt()));
        result.put("totalVotes", totalVotes);
        result.put("options", options);
        result.put("hasVoted", !myVotedOptionIds.isEmpty());
        return result;
    }

    /** 관리자가 옵션 삭제. 해당 옵션의 투표도 함께 삭제. 최소 2개 유지. */
    @Transactional
    public void deleteOption(Long surveyId, Long optionId, User admin) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 삭제할 수 있습니다.");

        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        SurveyOption opt = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다."));

        if (!opt.getSurvey().getId().equals(surveyId))
            throw new IllegalArgumentException("옵션이 이 설문에 속하지 않습니다.");

        if (optionRepository.countBySurveyId(surveyId) <= 2)
            throw new IllegalArgumentException("최소 2개의 옵션이 필요합니다.");

        voteRepository.deleteByOptionId(optionId);
        optionRepository.deleteById(optionId);

        if (opt.getImageUrl() != null) {
            Post post = survey.getPost();
            postImageRepository.findFirstByPostIdAndImageUrl(post.getId(), opt.getImageUrl())
                .ifPresent(postImageRepository::delete);
            if (opt.getImageUrl().equals(post.getImageUrl())) {
                List<com.vgc.entity.PostImage> imgs = postImageRepository.findByPostIdOrderBySortOrder(post.getId());
                post.setImageUrl(imgs.isEmpty() ? null : imgs.get(0).getImageUrl());
                postRepository.save(post);
            }
        }
    }

    /** 관리자가 기존 설문에 새 옵션 추가. */
    @Transactional
    public SurveyOption addAdminOption(Long surveyId, User admin,
                                       String text, org.springframework.web.multipart.MultipartFile image) throws IOException {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 옵션을 추가할 수 있습니다.");

        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        boolean hasText = text != null && !text.isBlank();
        boolean hasImage = image != null && !image.isEmpty();
        if (!hasText && !hasImage)
            throw new IllegalArgumentException("텍스트 또는 이미지 중 하나는 필요합니다.");
        if (hasText && text.length() > MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("옵션 텍스트는 " + MAX_TEXT_LENGTH + "자 이하여야 합니다.");

        long total = optionRepository.countBySurveyId(surveyId);
        if (total >= MAX_ADMIN_OPTIONS)
            throw new IllegalArgumentException("관리자 옵션은 최대 " + MAX_ADMIN_OPTIONS + "개까지 가능합니다.");

        SurveyOptionType type = hasText && hasImage ? SurveyOptionType.TEXT_AND_IMAGE
            : hasText ? SurveyOptionType.TEXT_ONLY : SurveyOptionType.IMAGE_ONLY;

        SurveyOption opt = new SurveyOption();
        opt.setSurvey(survey);
        opt.setOptionType(type);
        opt.setDisplayOrder((int) total);
        if (hasText) opt.setTextContent(text.trim());
        if (hasImage) {
            String url = imageStorageService.upload(image);
            opt.setImageUrl(url);
            Post post = survey.getPost();
            List<com.vgc.entity.PostImage> imgs = postImageRepository.findByPostIdOrderBySortOrder(post.getId());
            if (imgs.size() < MAX_THUMBNAIL_IMAGES) {
                postImageRepository.save(new com.vgc.entity.PostImage(post, url, imgs.size()));
                if (post.getImageUrl() == null) { post.setImageUrl(url); postRepository.save(post); }
            }
        }
        return optionRepository.save(opt);
    }

    /** 관리자가 게시글 ID로 설문 즉시 종료. */
    @Transactional
    public void closeSurveyByPost(Long postId, User admin) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 종료할 수 있습니다.");
        Survey survey = surveyRepository.findByPostId(postId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));
        if (LocalDateTime.now().isAfter(survey.getClosesAt()))
            throw new IllegalArgumentException("이미 종료된 설문입니다.");
        survey.setClosesAt(LocalDateTime.now().minusSeconds(1));
        surveyRepository.save(survey);
    }

    /** 관리자가 설문 옵션의 텍스트/이미지를 수정. */
    @Transactional
    public void updateSurveyOption(Long surveyId, Long optionId, User admin,
                                   String text, org.springframework.web.multipart.MultipartFile image) throws IOException {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 수정할 수 있습니다.");

        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        SurveyOption opt = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다."));

        if (!opt.getSurvey().getId().equals(surveyId))
            throw new IllegalArgumentException("옵션이 이 설문에 속하지 않습니다.");

        if (text != null && opt.getOptionType() != SurveyOptionType.IMAGE_ONLY) {
            if (text.length() > MAX_TEXT_LENGTH)
                throw new IllegalArgumentException("옵션 텍스트는 " + MAX_TEXT_LENGTH + "자 이하여야 합니다.");
            opt.setTextContent(text.isBlank() ? null : text.trim());
        }

        if (image != null && !image.isEmpty() && opt.getOptionType() != SurveyOptionType.TEXT_ONLY) {
            String oldUrl = opt.getImageUrl();
            String newUrl = imageStorageService.upload(image);
            opt.setImageUrl(newUrl);

            Post post = survey.getPost();
            if (oldUrl != null) {
                postImageRepository.findFirstByPostIdAndImageUrl(post.getId(), oldUrl)
                    .ifPresent(pi -> { pi.setImageUrl(newUrl); postImageRepository.save(pi); });
                if (oldUrl.equals(post.getImageUrl())) {
                    post.setImageUrl(newUrl);
                    postRepository.save(post);
                }
            }
        }

        optionRepository.save(opt);
    }

    /** 관리자가 설문 제목/종료일을 수정. */
    @Transactional
    public void updateSurveyMeta(Long surveyId, User admin, String title, LocalDateTime closesAt) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 수정할 수 있습니다.");

        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        if (title != null && !title.isBlank()) {
            survey.getPost().setTitle(title.trim());
            postRepository.save(survey.getPost());
        }
        if (closesAt != null && closesAt.isAfter(LocalDateTime.now())) {
            survey.setClosesAt(closesAt);
            surveyRepository.save(survey);
        }
    }

    /** 관리자가 설문을 즉시 종료. closes_at 을 현재 시각으로 당긴다. */
    @Transactional
    public void closeSurvey(Long surveyId, User admin) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 설문을 종료할 수 있습니다.");
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));
        if (LocalDateTime.now().isAfter(survey.getClosesAt()))
            throw new IllegalArgumentException("이미 종료된 설문입니다.");
        survey.setClosesAt(LocalDateTime.now().minusSeconds(1));
        surveyRepository.save(survey);
    }

    /** 관리자 전용 — 설문 ID 기준 옵션별 투표자 목록 조회. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVoteDetails(Long surveyId, User admin) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new IllegalArgumentException("관리자만 조회할 수 있습니다.");

        surveyRepository.findById(surveyId)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다."));

        List<SurveyOption> options = optionRepository.findBySurveyIdOrderByDisplayOrderAscIdAsc(surveyId);

        Map<Long, List<SurveyVote>> byOption = voteRepository.findBySurveyId(surveyId)
            .stream().collect(java.util.stream.Collectors.groupingBy(v -> v.getOption().getId()));

        return options.stream().map(o -> {
            List<SurveyVote> votes = byOption.getOrDefault(o.getId(), Collections.emptyList());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("optionId", o.getId());
            m.put("text", o.getTextContent());
            m.put("imageUrl", o.getImageUrl());
            m.put("voteCount", votes.size());
            m.put("voters", votes.stream().map(v -> {
                Map<String, Object> voter = new LinkedHashMap<>();
                voter.put("userId", v.getUser().getId());
                voter.put("nickname", v.getUser().getNickname());
                voter.put("name", v.getUser().getName());
                voter.put("votedAt", v.getCreatedAt().toString());
                return voter;
            }).toList());
            return m;
        }).toList();
    }

    /** 현재 활성 공지 배너 목록. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveNoticeBanners() {
        LocalDateTime now = LocalDateTime.now();
        return surveyRepository.findActiveNotices(now).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("surveyId", s.getId());
            m.put("postId", s.getPost().getId());
            m.put("title", s.getPost().getTitle());
            m.put("closesAt", s.getClosesAt().toString());
            return m;
        }).toList();
    }

    /** 옵션 JSON 파싱 유틸 (컨트롤러에서 사용). */
    public List<SurveyOptionInput> parseOptionsJson(String optionsJson) throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode arr = om.readTree(optionsJson);
        if (!arr.isArray()) throw new IllegalArgumentException("options JSON 형식 오류");
        List<SurveyOptionInput> inputs = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonNode node = arr.get(i);
            SurveyOptionInput in = new SurveyOptionInput();
            in.setType(SurveyOptionType.valueOf(node.get("type").asText()));
            in.setText(node.has("text") && !node.get("text").isNull() ? node.get("text").asText(null) : null);
            inputs.add(in);
        }
        return inputs;
    }
}
