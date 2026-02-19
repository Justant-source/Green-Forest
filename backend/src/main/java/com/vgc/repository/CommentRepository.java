package com.vgc.repository;

import com.vgc.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
    int countByPostId(Long postId);
    void deleteByPostId(Long postId);
}
