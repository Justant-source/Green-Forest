package com.vgc.controller;

import com.vgc.dto.SurveyCreateRequest;
import com.vgc.dto.SurveyOptionInput;
import com.vgc.entity.SurveyOption;
import com.vgc.entity.Survey;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.SurveyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    private final UserRepository userRepository;

    public SurveyController(SurveyService surveyService, UserRepository userRepository) {
        this.surveyService = surveyService;
        this.userRepository = userRepository;
    }

    /**
     * 설문 + 게시글 생성 (로그인 회원). 공지/배송/참여자추가는 관리자만 서버에서 반영.
     * multipart/form-data:
     *   title, closesAt (ISO datetime), anonymous, allowOptionAddByUser, allowMultiSelect, notice: boolean
     *   options: JSON array of {type, text, description}
     *   optionImage_0, optionImage_1, ...: MultipartFile (IMAGE_ONLY / TEXT_AND_IMAGE 옵션용)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createSurvey(
            @RequestParam("title") String title,
            @RequestParam("closesAt") String closesAtIso,
            @RequestParam(value = "anonymous", defaultValue = "false") boolean anonymous,
            @RequestParam(value = "allowOptionAddByUser", defaultValue = "false") boolean allowOptionAddByUser,
            @RequestParam(value = "allowMultiSelect", defaultValue = "false") boolean allowMultiSelect,
            @RequestParam(value = "notice", defaultValue = "false") boolean notice,
            @RequestParam(value = "requiresShipping", defaultValue = "false") boolean requiresShipping,
            @RequestParam("options") String optionsJson,
            MultipartHttpServletRequest mreq,
            Authentication authentication) throws IOException {

        User actor = currentUser(authentication);

        List<SurveyOptionInput> inputs = surveyService.parseOptionsJson(optionsJson);
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).setImage(mreq.getFile("optionImage_" + i));
        }

        SurveyCreateRequest req = new SurveyCreateRequest();
        req.setClosesAt(parseClosesAt(closesAtIso));
        req.setAnonymous(anonymous);
        req.setAllowOptionAddByUser(allowOptionAddByUser);
        req.setAllowMultiSelect(allowMultiSelect);
        req.setNotice(notice);
        req.setRequiresShipping(requiresShipping);

        Survey created = surveyService.createSurveyWithPost(actor, title, req, inputs);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("postId", created.getPost().getId());
        resp.put("surveyId", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/by-post/{postId}")
    public Map<String, Object> getByPost(@PathVariable Long postId, Authentication auth) {
        User user = auth != null
            ? userRepository.findByEmail(auth.getName()).orElse(null)
            : null;
        return surveyService.getSurveyDetail(postId, user);
    }

    @PostMapping("/{surveyId}/vote")
    public ResponseEntity<Void> vote(@PathVariable Long surveyId,
                                     @RequestBody Map<String, Object> body,
                                     Authentication auth) {
        User user = currentUser(auth);
        Object optionIdObj = body.get("optionId");
        if (optionIdObj == null) throw new IllegalArgumentException("optionId 필요");
        Long optionId = ((Number) optionIdObj).longValue();
        String recipientName = (String) body.get("recipientName");
        surveyService.vote(surveyId, optionId, user, recipientName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{surveyId}/options")
    public ResponseEntity<Map<String, Object>> addUserOption(
            @PathVariable Long surveyId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        User user = currentUser(auth);
        SurveyOption opt = surveyService.addUserOption(surveyId, user, body.get("text"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", opt.getId(),
            "type", opt.getOptionType().name(),
            "text", opt.getTextContent(),
            "imageUrl", (Object) null,
            "voteCount", 0L,
            "voted", false,
            "addedByUser", true
        ));
    }

    /** 게시글 ID로 설문 즉시 종료 (작성자 또는 관리자). */
    @PatchMapping("/close-by-post/{postId}")
    public ResponseEntity<Void> closeSurveyByPost(@PathVariable Long postId, Authentication auth) {
        surveyService.closeSurveyByPost(postId, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    /** 옵션 삭제 (작성자 또는 관리자). */
    @DeleteMapping("/{surveyId}/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long surveyId,
                                              @PathVariable Long optionId,
                                              Authentication auth) {
        surveyService.deleteOption(surveyId, optionId, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    /** 작성자 또는 관리자가 기존 설문에 옵션 추가 (multipart). */
    @PostMapping(value = "/{surveyId}/admin-options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addAdminOption(
            @PathVariable Long surveyId,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication auth) throws IOException {
        SurveyOption opt = surveyService.addAdminOption(surveyId, currentUser(auth), text, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", opt.getId(),
            "type", opt.getOptionType().name(),
            "text", (Object)(opt.getTextContent() != null ? opt.getTextContent() : ""),
            "imageUrl", (Object) opt.getImageUrl(),
            "voteCount", 0L,
            "voted", false,
            "addedByUser", false
        ));
    }

    /** 설문 제목/종료일/배송여부 수정 (작성자 또는 관리자. 배송여부는 관리자만 반영). */
    @PatchMapping("/{surveyId}/meta")
    public ResponseEntity<Void> updateMeta(
            @PathVariable Long surveyId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        User actor = currentUser(auth);
        String title = body.get("title");
        String closesAtStr = body.get("closesAt");
        LocalDateTime closesAt = closesAtStr != null && !closesAtStr.isBlank()
            ? parseClosesAt(closesAtStr) : null;
        Boolean requiresShipping = body.containsKey("requiresShipping")
            ? Boolean.parseBoolean(body.get("requiresShipping")) : null;
        surveyService.updateSurveyMeta(surveyId, actor, title, closesAt, requiresShipping);
        return ResponseEntity.noContent().build();
    }

    /** 옵션 텍스트/이미지/설명 수정 (작성자 또는 관리자, multipart). */
    @PatchMapping(value = "/{surveyId}/options/{optionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateOption(
            @PathVariable Long surveyId,
            @PathVariable Long optionId,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth) throws IOException {
        surveyService.updateSurveyOption(surveyId, optionId, currentUser(auth), text, image, description);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{surveyId}/close")
    public ResponseEntity<Void> closeSurvey(@PathVariable Long surveyId, Authentication auth) {
        surveyService.closeSurvey(surveyId, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    /** 관리자, 또는 비익명 설문의 작성자 — 옵션별 투표자 목록. */
    @GetMapping("/{surveyId}/votes")
    public List<Map<String, Object>> getVotes(@PathVariable Long surveyId, Authentication auth) {
        return surveyService.getVoteDetails(surveyId, currentUser(auth));
    }

    @GetMapping("/notices")
    public List<Map<String, Object>> notices() {
        return surveyService.getActiveNoticeBanners();
    }

    private static LocalDateTime parseClosesAt(String raw) {
        String s = raw.trim().replace("Z", "");
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        if (s.length() == 16) s = s + ":00";
        return LocalDateTime.parse(s);
    }

    private User currentAdmin(Authentication auth) {
        User u = currentUser(auth);
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("관리자 권한이 필요합니다.");
        return u;
    }

    private User currentUser(Authentication auth) {
        if (auth == null) throw new RuntimeException("로그인이 필요합니다.");
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
