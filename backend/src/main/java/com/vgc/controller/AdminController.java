package com.vgc.controller;

import com.vgc.dto.CategoryRequestResponse;
import com.vgc.dto.CategoryResponse;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public AdminController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    private User getAdminUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        return user;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories(Authentication authentication) {
        getAdminUser(authentication);
        return categoryService.getAllCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        getAdminUser(authentication);
        String name = (String) body.get("name");
        String label = (String) body.get("label");
        String color = (String) body.get("color");
        boolean hasStatus = Boolean.TRUE.equals(body.get("hasStatus"));
        return ResponseEntity.ok(categoryService.createCategory(name, label, color, hasStatus));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {
        getAdminUser(authentication);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/category-requests")
    public List<CategoryRequestResponse> getPendingRequests(Authentication authentication) {
        getAdminUser(authentication);
        return categoryService.getPendingRequests();
    }

    @PostMapping("/category-requests/{id}/approve")
    public ResponseEntity<CategoryResponse> approveRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        getAdminUser(authentication);
        String label = (String) body.get("label");
        String color = (String) body.get("color");
        boolean hasStatus = Boolean.TRUE.equals(body.get("hasStatus"));
        return ResponseEntity.ok(categoryService.approveRequest(id, label, color, hasStatus));
    }

    @PostMapping("/category-requests/{id}/reject")
    public ResponseEntity<CategoryRequestResponse> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        getAdminUser(authentication);
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(categoryService.rejectRequest(id, reason));
    }
}
