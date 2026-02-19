package com.vgc.service;

import com.vgc.dto.CommentRequest;
import com.vgc.entity.Comment;
import com.vgc.entity.Post;
import com.vgc.entity.User;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId);
    }

    public int getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @Transactional
    public Comment addComment(Long postId, CommentRequest request, User author) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setContent(request.getContent());
        comment.setAuthorName(author.getNickname());
        comment.setAuthor(author);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (isAncestorDeleted(parent)) {
                throw new RuntimeException("Cannot reply to a deleted comment thread");
            }
            comment.setParent(parent);
        }

        return commentRepository.save(comment);
    }

    @Transactional
    public Comment updateComment(Long commentId, String content, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to update this comment");
        }
        if (comment.isDeleted()) {
            throw new RuntimeException("Cannot update a deleted comment");
        }
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to delete this comment");
        }
        comment.setDeleted(true);
        comment.setContent("삭제된 메시지입니다");
        return commentRepository.save(comment);
    }

    private boolean isAncestorDeleted(Comment comment) {
        Comment current = comment;
        while (current != null) {
            if (current.isDeleted()) return true;
            current = current.getParent();
        }
        return false;
    }
}
