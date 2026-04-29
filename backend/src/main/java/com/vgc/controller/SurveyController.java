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
     * 설문 + 게시글 생성 (관리자 전용).
     * multipart/form-data:
     *   title, closesAt (ISO datetime), anonymous, allowOptionAddByUser, allowMultiSelect, notice: boolean
     *   options: JSON array of {type, text}
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
            @RequestParam("options") String optionsJson,
            MultipartHttpServletRequest mreq,
            Authentication authentication) throws IOException {

        User admin = currentAdmin(authentication);

        List<SurveyOptionInput> inputs = surveyService.parseOptionsJson(optionsJson);
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).setImage(mreq.getFile("optionImage_" + i));
        }

        SurveyCreateRequest req = new SurveyCreateRequest();
        req.setClosesAt(LocalDateTime.parse(closesAtIso));
        req.setAnonymous(anonymous);
        req.setAllowOptionAddByUser(allowOptionAddByUser);
        req.setAllowMultiSelect(allowMultiSelect);
        req.setNotice(notice);

        Survey created = surveyService.createSurveyWithPost(admin, title, req, inputs);

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
                                     @RequestBody Map<String, Long> body,
                                     Authentication auth) {
        User user = currentUser(auth);
        Long optionId = body.get("optionId");
        if (optionId == null) throw new IllegalArgumentException("optionId 필요");
        surveyService.vote(surveyId, optionId, user);
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

    /** 게시글 ID로 설문 즉시 종료 (관리자 전용). */
    @PatchMapping("/close-by-post/{postId}")
    public ResponseEntity<Void> closeSurveyByPost(@PathVariable Long postId, Authentication auth) {
        surveyService.closeSurveyByPost(postId, currentAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    /** 옵션 삭제 (관리자 전용). */
    @DeleteMapping("/{surveyId}/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long surveyId,
                                              @PathVariable Long optionId,
                                              Authentication auth) {
        surveyService.deleteOption(surveyId, optionId, currentAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    /** 관리자가 기존 설문에 옵션 추가 (multipart). */
    @PostMapping(value = "/{surveyId}/admin-options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addAdminOption(
            @PathVariable Long surveyId,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication auth) throws IOException {
        SurveyOption opt = surveyService.addAdminOption(surveyId, currentAdmin(auth), text, image);
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

    /** 설문 제목/종료일 수정 (관리자 전용). */
    @PatchMapping("/{surveyId}/meta")
    public ResponseEntity<Void> updateMeta(
            @PathVariable Long surveyId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        User admin = currentAdmin(auth);
        String title = body.get("title");
        String closesAtStr = body.get("closesAt");
        LocalDateTime closesAt = closesAtStr != null ? LocalDateTime.parse(closesAtStr) : null;
        surveyService.updateSurveyMeta(surveyId, admin, title, closesAt);
        return ResponseEntity.noContent().build();
    }

    /** 옵션 텍스트/이미지 수정 (관리자 전용, multipart). */
    @PatchMapping(value = "/{surveyId}/options/{optionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateOption(
            @PathVariable Long surveyId,
            @PathVariable Long optionId,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication auth) throws IOException {
        surveyService.updateSurveyOption(surveyId, optionId, currentAdmin(auth), text, image);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{surveyId}/close")
    public ResponseEntity<Void> closeSurvey(@PathVariable Long surveyId, Authentication auth) {
        surveyService.closeSurvey(surveyId, currentAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    /** 관리자 전용 — 옵션별 투표자 목록. */
    @GetMapping("/{surveyId}/votes")
    public List<Map<String, Object>> getVotes(@PathVariable Long surveyId, Authentication auth) {
        return surveyService.getVoteDetails(surveyId, currentAdmin(auth));
    }

    @GetMapping("/notices")
    public List<Map<String, Object>> notices() {
        return surveyService.getActiveNoticeBanners();
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
