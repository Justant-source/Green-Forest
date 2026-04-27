package com.vgc.controller.event.photobingo;

import com.vgc.dto.event.EventResponse;
import com.vgc.dto.event.photobingo.PhotoBingoScoringRequest;
import com.vgc.dto.event.photobingo.PhotoBingoSubmissionResponse;
import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.UserRepository;
import com.vgc.service.event.photobingo.PhotoBingoScoringService;
import com.vgc.service.event.photobingo.PhotoBingoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events/{eventId}")
public class PhotoBingoAdminController {

    private final PhotoBingoService photoBingoService;
    private final PhotoBingoScoringService scoringService;
    private final UserRepository userRepository;

    public PhotoBingoAdminController(PhotoBingoService photoBingoService,
                                     PhotoBingoScoringService scoringService,
                                     UserRepository userRepository) {
        this.photoBingoService = photoBingoService;
        this.scoringService = scoringService;
        this.userRepository = userRepository;
    }

    private User requireAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return user;
    }

    @GetMapping("/photo-bingo/submissions")
    public List<PhotoBingoSubmissionResponse> listSubmissions(@PathVariable Long eventId, Authentication auth) {
        requireAdmin(auth);
        return photoBingoService.listAllSubmissions(eventId).stream()
                .map(PhotoBingoSubmissionResponse::from)
                .toList();
    }

    @GetMapping("/photo-bingo/submissions/{submissionId}")
    public PhotoBingoSubmissionResponse getSubmission(@PathVariable Long eventId,
                                                      @PathVariable Long submissionId,
                                                      Authentication auth) {
        requireAdmin(auth);
        PhotoBingoSubmission sub = photoBingoService.getSubmissionById(eventId, submissionId);
        return PhotoBingoSubmissionResponse.from(sub);
    }

    @PutMapping("/photo-bingo/cells/{cellId}/score")
    public PhotoBingoSubmissionResponse scoreCell(@PathVariable Long eventId,
                                                  @PathVariable Long cellId,
                                                  @RequestBody PhotoBingoScoringRequest req,
                                                  Authentication auth) {
        User admin = requireAdmin(auth);
        PhotoBingoCell cell = scoringService.scoreCell(eventId, cellId, req.getScoreStatus(), req.getComment(), admin);
        return PhotoBingoSubmissionResponse.from(cell.getSubmission());
    }

    @PostMapping("/finalize")
    public EventResponse finalizeEvent(@PathVariable Long eventId, Authentication auth) {
        User admin = requireAdmin(auth);
        Event event = scoringService.finalizeEvent(eventId, admin);
        return EventResponse.from(event);
    }
}
