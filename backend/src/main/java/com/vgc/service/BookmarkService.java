package com.vgc.service;

import com.vgc.dto.PostResponse;
import com.vgc.entity.Bookmark;
import com.vgc.entity.Post;
import com.vgc.entity.User;
import com.vgc.repository.BookmarkRepository;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, PostRepository postRepository, CommentRepository commentRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public boolean toggleBookmark(User user, Long postId) {
        if (bookmarkRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            bookmarkRepository.deleteByUserIdAndPostId(user.getId(), postId);
            return false;
        } else {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setPost(post);
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    public boolean isBookmarked(Long userId, Long postId) {
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    public Page<PostResponse> getBookmarkedPosts(Long userId, Pageable pageable) {
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Long> postIds = bookmarks.getContent().stream()
                .map(bookmark -> bookmark.getPost().getId()).collect(Collectors.toList());
        Map<Long, Long> commentCountMap = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return bookmarks.map(bookmark -> {
            Post post = bookmark.getPost();
            PostResponse response = PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0L).intValue());
            response.setBookmarked(true);
            return response;
        });
    }
}
