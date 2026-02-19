package com.vgc.dto;

import com.vgc.entity.Post;
import com.vgc.entity.PostImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private String category;
    private int likeCount;
    private int viewCount;
    private LocalDateTime createdAt;
    private int commentCount;
    private String authorNickname;
    private boolean bookmarked;
    private boolean liked;
    private String status;
    private List<String> imageUrls;

    public static PostResponse from(Post post, int commentCount) {
        PostResponse response = new PostResponse();
        response.id = post.getId();
        response.title = post.getTitle();
        response.content = post.getContent();
        response.imageUrl = post.getImageUrl();
        response.category = post.getCategory();
        response.likeCount = post.getLikeCount();
        response.viewCount = post.getViewCount();
        response.createdAt = post.getCreatedAt();
        response.commentCount = commentCount;
        response.authorNickname = post.getAuthor() != null ? post.getAuthor().getNickname() : null;
        response.status = post.getStatus() != null ? post.getStatus().name() : null;
        response.imageUrls = post.getImages() != null
                ? post.getImages().stream().map(PostImage::getImageUrl).collect(Collectors.toList())
                : List.of();
        return response;
    }

    // All getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public int getLikeCount() { return likeCount; }
    public int getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getCommentCount() { return commentCount; }
    public String getAuthorNickname() { return authorNickname; }
    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
