package com.vgc.controller;

import com.vgc.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public List<Map<String, Object>> getPartyRankings(
            @RequestParam(defaultValue = "all_time") String period) {
        return leaderboardService.getPartyRankings(period);
    }

    @GetMapping("/party/{partyId}")
    public List<Map<String, Object>> getPartyMemberRankings(
            @PathVariable Long partyId,
            @RequestParam(defaultValue = "all_time") String period) {
        return leaderboardService.getPartyMemberRankings(partyId, period);
    }
}
