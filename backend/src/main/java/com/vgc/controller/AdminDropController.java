package com.vgc.controller;

import com.vgc.dto.AdminDropTransactionResponse;
import com.vgc.entity.DropReasonType;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.AdminDropService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/drop-transactions")
public class AdminDropController {

    private final AdminDropService adminDropService;
    private final UserRepository userRepository;

    public AdminDropController(AdminDropService adminDropService, UserRepository userRepository) {
        this.adminDropService = adminDropService;
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

    private List<DropReasonType> parseTypes(String types) {
        if (types == null || types.isBlank()) return List.of();
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return DropReasonType.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 reasonType: " + s);
                    }
                })
                .toList();
    }

    @GetMapping
    public Page<AdminDropTransactionResponse> list(
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication auth) {
        requireAdmin(auth);
        return adminDropService.list(parseTypes(types), page, size);
    }

    @PostMapping("/{id}/revoke")
    public Map<String, String> revoke(@PathVariable Long id, Authentication auth) {
        User admin = requireAdmin(auth);
        adminDropService.revoke(id, admin);
        return Map.of("message", "회수 완료");
    }

    @PatchMapping("/{id}")
    public AdminDropTransactionResponse adjust(@PathVariable Long id,
                                               @RequestBody AdjustRequest req,
                                               Authentication auth) {
        User admin = requireAdmin(auth);
        return adminDropService.adjust(id, req.amount, admin);
    }

    public static class AdjustRequest {
        public int amount;
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }
}
