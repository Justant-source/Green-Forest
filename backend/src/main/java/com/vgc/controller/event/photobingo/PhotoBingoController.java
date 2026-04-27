package com.vgc.controller.event.photobingo;

import com.vgc.dto.event.photobingo.CaptionUpdateRequest;
import com.vgc.dto.event.photobingo.PhotoBingoActivityResponse;
import com.vgc.dto.event.photobingo.PhotoBingoSubmissionResponse;
import com.vgc.entity.User;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.UserRepository;
import com.vgc.service.event.photobingo.PhotoBingoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/events/{eventId}/photo-bingo")
public class PhotoBingoController {

    private final PhotoBingoService photoBingoService;
    private final UserRepository userRepository;

    public PhotoBingoController(PhotoBingoService photoBingoService, UserRepository userRepository) {
        this.photoBingoService = photoBingoService;
        this.userRepository = userRepository;
    }

    private User requireUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    @GetMapping("/my-submission")
    public PhotoBingoSubmissionResponse getMySubmission(@PathVariable Long eventId, Authentication auth) {
        User user = requireUser(auth);
        PhotoBingoSubmission sub = photoBingoService.getOrCreateMySubmission(eventId, user);
        return PhotoBingoSubmissionResponse.from(sub);
    }

    @PatchMapping("/my-submission")
    public PhotoBingoSubmissionResponse updateCaption(@PathVariable Long eventId,
                                                      @RequestBody CaptionUpdateRequest req,
                                                      Authentication auth) {
        User user = requireUser(auth);
        PhotoBingoSubmission sub = photoBingoService.updateCaption(eventId, user, req.getCaption());
        return PhotoBingoSubmissionResponse.from(sub);
    }

    @PutMapping(value = "/cells/{cellIndex}/image", consumes = "multipart/form-data")
    public PhotoBingoSubmissionResponse uploadCellImage(@PathVariable Long eventId,
                                                        @PathVariable int cellIndex,
                                                        @RequestParam("image") MultipartFile image,
                                                        Authentication auth) {
        User user = requireUser(auth);
        PhotoBingoSubmission sub = photoBingoService.uploadCellImage(eventId, cellIndex, image, user);
        return PhotoBingoSubmissionResponse.from(sub);
    }

    @DeleteMapping("/cells/{cellIndex}/image")
    public PhotoBingoSubmissionResponse deleteCellImage(@PathVariable Long eventId,
                                                        @PathVariable int cellIndex,
                                                        Authentication auth) {
        User user = requireUser(auth);
        PhotoBingoSubmission sub = photoBingoService.deleteCellImage(eventId, cellIndex, user);
        return PhotoBingoSubmissionResponse.from(sub);
    }

    /**
     * 최근 업로드 활동 피드 (티커 렌더용). 참여자 모두가 공유하는 공개 피드.
     * 각 item의 uploadedCount 는 해당 유저가 지금까지 업로드한 셀 수 (1~9).
     */
    @GetMapping("/activity")
    public List<PhotoBingoActivityResponse> getActivity(@PathVariable Long eventId,
                                                        @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 50);
        List<PhotoBingoCell> recent = photoBingoService.listRecentUploads(eventId, safeLimit);

        Map<Long, Integer> uploadedCountCache = new ConcurrentHashMap<>();
        return recent.stream().map(c -> {
            PhotoBingoSubmission s = c.getSubmission();
            int total = uploadedCountCache.computeIfAbsent(s.getId(),
                    k -> (int) s.getCells().stream().filter(x -> x.getImageUrl() != null).count());
            return new PhotoBingoActivityResponse(
                    s.getUser().getId(),
                    s.getUser().getNickname(),
                    c.getCellIndex(),
                    c.getTheme(),
                    c.getUploadedAt(),
                    total
            );
        }).toList();
    }
}
