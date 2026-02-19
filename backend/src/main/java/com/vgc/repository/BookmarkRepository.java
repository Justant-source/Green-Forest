package com.vgc.repository;

import com.vgc.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    void deleteByPostId(Long postId);
    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
