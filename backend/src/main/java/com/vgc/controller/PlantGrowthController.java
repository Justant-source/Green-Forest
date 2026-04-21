package com.vgc.controller;

import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.PlantGrowthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plant")
public class PlantGrowthController {

    private final PlantGrowthService plantGrowthService;
    private final UserRepository userRepository;

    public PlantGrowthController(PlantGrowthService plantGrowthService, UserRepository userRepository) {
        this.plantGrowthService = plantGrowthService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyGrowth(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(plantGrowthService.getMyGrowth(user.getId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserGrowth(@PathVariable Long userId) {
        return ResponseEntity.ok(plantGrowthService.getUserGrowth(userId));
    }
}
