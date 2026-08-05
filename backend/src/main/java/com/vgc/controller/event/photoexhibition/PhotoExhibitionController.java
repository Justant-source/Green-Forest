package com.vgc.controller.event.photoexhibition;

import com.vgc.dto.event.photoexhibition.PhotoExhibitionSubmissionResponse;
import com.vgc.entity.User;
import com.vgc.entity.event.EventStatus;
import com.vgc.repository.UserRepository;
import com.vgc.service.event.photoexhibition.PhotoExhibitionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/{eventId}/photo-exhibition")
public class PhotoExhibitionController {
    private final PhotoExhibitionService service;
    private final UserRepository users;
    public PhotoExhibitionController(PhotoExhibitionService service, UserRepository users) { this.service = service; this.users = users; }
    private User user(Authentication auth) { if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."); return users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.")); }
    @GetMapping("/my-submission") public PhotoExhibitionSubmissionResponse mine(@PathVariable Long eventId, Authentication auth) { return PhotoExhibitionSubmissionResponse.from(service.mine(eventId, user(auth)), true, false); }
    @PatchMapping("/my-submission") public PhotoExhibitionSubmissionResponse update(@PathVariable Long eventId, @RequestBody Map<String, String> body, Authentication auth) { return PhotoExhibitionSubmissionResponse.from(service.update(eventId, user(auth), body.get("title"), body.get("introduction")), true, false); }
    @PostMapping(value = "/images", consumes = "multipart/form-data") public PhotoExhibitionSubmissionResponse upload(@PathVariable Long eventId, @RequestParam MultipartFile image, Authentication auth) { return PhotoExhibitionSubmissionResponse.from(service.upload(eventId, user(auth), image), true, false); }
    @DeleteMapping("/images/{imageId}") public PhotoExhibitionSubmissionResponse deleteImage(@PathVariable Long eventId, @PathVariable Long imageId, Authentication auth) { return PhotoExhibitionSubmissionResponse.from(service.deleteImage(eventId, user(auth), imageId), true, false); }
    @PutMapping("/images/order") public PhotoExhibitionSubmissionResponse orderImages(@PathVariable Long eventId, @RequestBody Map<String, List<Long>> body, Authentication auth) { return PhotoExhibitionSubmissionResponse.from(service.reorderImages(eventId, user(auth), body.get("imageIds")), true, false); }
    @GetMapping("/gallery") public List<PhotoExhibitionSubmissionResponse> gallery(@PathVariable Long eventId, Authentication auth) { User current = auth == null ? null : user(auth); boolean published = service.status(eventId) == EventStatus.SCORED; return service.gallery(eventId, false, current == null ? null : current.getId()).stream().map(submission -> PhotoExhibitionSubmissionResponse.from(submission, current != null && submission.getUser().getId().equals(current.getId()), published)).toList(); }
    @PutMapping("/votes") public List<Long> vote(@PathVariable Long eventId, @RequestBody Map<String, List<Long>> body, Authentication auth) { return service.vote(eventId, user(auth), body.get("submissionIds")); }
    @GetMapping("/my-votes") public List<Long> myVotes(@PathVariable Long eventId, Authentication auth) { return service.myVoteIds(eventId, user(auth)); }
}
