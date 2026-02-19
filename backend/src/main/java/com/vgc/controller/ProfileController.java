package com.vgc.controller;

import com.vgc.dto.PostResponse;
import com.vgc.entity.Post;
import com.vgc.entity.User;
import com.vgc.repository.BookmarkRepository;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostRepository;
import com.vgc.repository.UserRepository;
import com.vgc.service.BookmarkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookmarkService bookmarkService;
    private final BookmarkRepository bookmarkRepository;

    public ProfileController(PostRepository postRepository, CommentRepository commentRepository,
                             UserRepository userRepository, BookmarkService bookmarkService,
                             BookmarkRepository bookmarkRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.bookmarkService = bookmarkService;
        this.bookmarkRepository = bookmarkRepository;
    }

    @GetMapping("/posts")
    public Page<PostResponse> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
        List<Long> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Long> commentCountMap = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Set<Long> bookmarkedPostIds = new HashSet<>(bookmarkRepository.findPostIdsByUserIdAndPostIdIn(user.getId(), postIds));

        return posts.map(post -> {
            PostResponse response = PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0L).intValue());
            response.setBookmarked(bookmarkedPostIds.contains(post.getId()));
            return response;
        });
    }

    @GetMapping("/bookmarks")
    public Page<PostResponse> getMyBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return bookmarkService.getBookmarkedPosts(user.getId(), PageRequest.of(page, size));
    }
}
