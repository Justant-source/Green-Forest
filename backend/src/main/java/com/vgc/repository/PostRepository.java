package com.vgc.repository;

import com.vgc.entity.Post;
import com.vgc.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"author", "images"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "images"})
    Page<Post> findByCategory(String category, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "images"})
    Page<Post> findByCategoryAndStatus(String category, PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "images"})
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}
