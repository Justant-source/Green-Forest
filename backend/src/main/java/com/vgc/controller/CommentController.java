package com.vgc.controller;

import com.vgc.dto.CommentRequest;
import com.vgc.entity.Comment;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.CommentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;

    public CommentController(CommentService commentService, UserRepository userRepository) {
        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Comment> getComments(@PathVariable Long postId) {
        return commentService.getComments(postId);
    }

    @GetMapping("/count")
    public Map<String, Integer> getCommentCount(@PathVariable Long postId) {
        return Map.of("count", commentService.getCommentCount(postId));
    }

    @PostMapping
    public Comment addComment(@PathVariable Long postId,
                              @RequestBody CommentRequest request,
                              Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return commentService.addComment(postId, request, user);
    }

    @PutMapping("/{commentId}")
    public Comment updateComment(@PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 @RequestBody CommentRequest request,
                                 Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return commentService.updateComment(commentId, request.getContent(), user);
    }

    @DeleteMapping("/{commentId}")
    public Comment deleteComment(@PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return commentService.deleteComment(commentId, user);
    }
}
