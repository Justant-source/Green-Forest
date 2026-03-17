package com.vgc.controller;

import com.vgc.entity.*;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.UserRepository;
import com.vgc.service.DropService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final DropTransactionRepository dropTransactionRepository;
    private final DropService dropService;

    public UserController(UserRepository userRepository,
                          DropTransactionRepository dropTransactionRepository,
                          DropService dropService) {
        this.userRepository = userRepository;
        this.dropTransactionRepository = dropTransactionRepository;
        this.dropService = dropService;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        return userToMap(user);
    }

    @GetMapping("/me")
    public Map<String, Object> getMe(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userToMap(user);
    }

    @PutMapping("/me/profile")
    public Map<String, Object> updateMyProfile(@RequestBody Map<String, Object> body,
                                                Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("nickname")) {
            String newNickname = (String) body.get("nickname");
            if (!newNickname.equals(user.getNickname()) && userRepository.existsByNickname(newNickname)) {
                throw new RuntimeException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(newNickname);
        }

        if (body.containsKey("plantName")) {
            user.setPlantName((String) body.get("plantName"));
        }

        if (body.containsKey("plantType") && body.get("plantType") != null) {
            if (user.getPlantType() != null && user.isPlantLocked()) {
                throw new RuntimeException("식물은 이미 선택되어 변경할 수 없습니다. 관리자에게 문의하세요.");
            }
            PlantType plantType = PlantType.valueOf((String) body.get("plantType"));
            user.setPlantType(plantType);
            user.setPlantLocked(true);

            // 식물 → 직업군 자동 매핑
            switch (plantType) {
                case TABLE_PALM -> {
                    user.setJobClass(JobClass.TANKER);
                    user.setElement(Element.EARTH);
                    user.setDifficulty(Difficulty.EASY);
                    user.setExpMultiplier(Difficulty.EASY.getMultiplier());
                }
                case SPATHIPHYLLUM -> {
                    user.setJobClass(JobClass.HEALER);
                    user.setElement(Element.WATER);
                    user.setDifficulty(Difficulty.EASY);
                    user.setExpMultiplier(Difficulty.EASY.getMultiplier());
                }
                case HONG_KONG_PALM -> {
                    user.setJobClass(JobClass.BUFFER);
                    user.setElement(Element.WIND);
                    user.setDifficulty(Difficulty.NORMAL);
                    user.setExpMultiplier(Difficulty.NORMAL.getMultiplier());
                }
                case ORANGE_JASMINE -> {
                    user.setJobClass(JobClass.DEALER);
                    user.setElement(Element.FIRE);
                    user.setDifficulty(Difficulty.HARD);
                    user.setExpMultiplier(Difficulty.HARD.getMultiplier());
                }
            }
        }

        userRepository.save(user);
        return userToMap(user);
    }

    @GetMapping("/me/drops")
    public Page<Map<String, Object>> getMyDropHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return dropTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", tx.getId());
                    map.put("amount", tx.getAmount());
                    map.put("reasonType", tx.getReasonType().name());
                    map.put("reasonLabel", tx.getReasonType().getLabel());
                    map.put("reasonDetail", tx.getReasonDetail());
                    map.put("relatedPostId", tx.getRelatedPostId());
                    map.put("relatedQuestId", tx.getRelatedQuestId());
                    map.put("createdAt", tx.getCreatedAt().toString());
                    return map;
                });
    }

    @PostMapping("/me/gift")
    public Map<String, String> giftDrops(@RequestBody Map<String, Object> body,
                                          Authentication authentication) {
        User sender = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String receiverNickname = (String) body.get("receiverNickname");
        int amount = ((Number) body.get("amount")).intValue();

        User receiver = userRepository.findByNickname(receiverNickname)
                .orElseThrow(() -> new RuntimeException("받는 유저를 찾을 수 없습니다."));

        dropService.giftDrops(sender, receiver, amount);
        return Map.of("status", "gifted", "newTotal", String.valueOf(sender.getTotalDrops()));
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        map.put("plantType", user.getPlantType() != null ? user.getPlantType().name() : null);
        map.put("plantTypeLabel", user.getPlantType() != null ? user.getPlantType().getLabel() : null);
        map.put("plantName", user.getPlantName());
        map.put("plantLocked", user.isPlantLocked());
        map.put("jobClass", user.getJobClass() != null ? user.getJobClass().name() : null);
        map.put("jobClassLabel", user.getJobClass() != null ? user.getJobClass().getLabel() : null);
        map.put("jobClassLabelEn", user.getJobClass() != null ? user.getJobClass().getLabelEn() : null);
        map.put("element", user.getElement() != null ? user.getElement().name() : null);
        map.put("elementLabel", user.getElement() != null ? user.getElement().getLabel() : null);
        map.put("difficulty", user.getDifficulty() != null ? user.getDifficulty().name() : null);
        map.put("difficultyLabel", user.getDifficulty() != null ? user.getDifficulty().getLabel() : null);
        map.put("expMultiplier", user.getExpMultiplier());
        map.put("partyId", user.getParty() != null ? user.getParty().getId() : null);
        map.put("partyName", user.getParty() != null ? user.getParty().getName() : null);
        map.put("totalDrops", user.getTotalDrops());
        map.put("createdAt", user.getCreatedAt().toString());
        return map;
    }
}
