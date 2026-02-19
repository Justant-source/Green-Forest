package com.vgc.repository;

import com.vgc.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPostIdOrderBySortOrder(Long postId);
    void deleteByPostId(Long postId);
}
