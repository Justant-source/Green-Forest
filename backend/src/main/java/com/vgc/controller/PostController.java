package com.vgc.controller;

import com.vgc.dto.PostRequest;
import com.vgc.dto.PostResponse;
import com.vgc.entity.PostStatus;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;

    public PostController(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<PostResponse> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return postService.getAllPosts(category, sort, status, page, size);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @PostMapping
    public PostResponse createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) throws Exception {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        PostRequest request = new PostRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setCategory(category);
        return postService.createPost(request, images, user);
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "existingImageUrls", required = false) List<String> existingImageUrls,
            Authentication authentication) throws Exception {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        PostRequest request = new PostRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setCategory(category);
        return postService.updatePost(id, request, images, existingImageUrls, user);
    }

    @PatchMapping("/{id}/status")
    public PostResponse updatePostStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        PostStatus status = PostStatus.valueOf(body.get("status"));
        return postService.updatePostStatus(id, status, user);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deletePost(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        postService.deletePost(id, user);
        return Map.of("message", "삭제되었습니다.");
    }

    @GetMapping("/{id}/like")
    public Map<String, Boolean> getLikeStatus(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean liked = postService.isLiked(user.getId(), id);
        return Map.of("liked", liked);
    }

    @PostMapping("/{id}/like")
    public PostResponse toggleLike(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return postService.toggleLike(id, user);
    }
}
