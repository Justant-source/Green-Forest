package com.vgc.controller;

import com.vgc.entity.GachaDraw;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.GachaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gacha")
public class GachaController {

    private final GachaService gachaService;
    private final UserRepository userRepository;

    public GachaController(GachaService gachaService, UserRepository userRepository) {
        this.gachaService = gachaService;
        this.userRepository = userRepository;
    }

    @GetMapping("/prizes")
    public ResponseEntity<List<Map<String, Object>>> listPrizes() {
        return ResponseEntity.ok(gachaService.listAvailablePrizes());
    }

    @PostMapping("/draw")
    public ResponseEntity<Map<String, Object>> draw(
            Authentication authentication,
            @RequestBody Map<String, Long> body) {
        User user = getUser(authentication);
        Long prizeId = body.get("prizeId");
        return ResponseEntity.ok(gachaService.draw(user.getId(), prizeId));
    }

    @GetMapping("/me/history")
    public ResponseEntity<Page<GachaDraw>> myHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUser(authentication);
        return ResponseEntity.ok(gachaService.getMyHistory(user.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/recent-wins")
    public ResponseEntity<List<Map<String, Object>>> recentWins(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gachaService.getRecentWins(limit));
    }

    @GetMapping("/me/quota")
    public ResponseEntity<Map<String, Object>> quota(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(gachaService.getRemainingDrawsToday(user.getId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(Authentication authentication) {
        User user = getUser(authentication);
        var dto = gachaService.getStats(user.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remaining", dto.getRemaining());
        result.put("dailyLimit", dto.getDailyLimit());
        result.put("todayDrawCount", dto.getTodayDrawCount());
        result.put("betCost", dto.getBetCost());
        result.put("expectedReward", dto.getExpectedReward());
        result.put("totalActivePrizes", dto.getTotalActivePrizes());
        return ResponseEntity.ok(result);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
