package com.vgc.repository;

import com.vgc.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    boolean existsByPostIdAndTaggedUserId(Long postId, Long taggedUserId);
    List<PostTag> findByPostId(Long postId);
    List<PostTag> findByTaggedUserId(Long taggedUserId);
    void deleteByPostId(Long postId);
}
