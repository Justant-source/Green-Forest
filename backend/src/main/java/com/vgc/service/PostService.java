package com.vgc.service;

import com.vgc.dto.PostRequest;
import com.vgc.dto.PostResponse;
import com.vgc.entity.Post;
import com.vgc.entity.PostImage;
import com.vgc.entity.PostLike;
import com.vgc.entity.PostStatus;
import com.vgc.entity.User;
import com.vgc.repository.BookmarkRepository;
import com.vgc.repository.CategoryRepository;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostImageRepository;
import com.vgc.repository.PostLikeRepository;
import com.vgc.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostImageRepository postImageRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    public PostService(PostRepository postRepository, CommentRepository commentRepository, PostLikeRepository postLikeRepository, PostImageRepository postImageRepository, BookmarkRepository bookmarkRepository, CategoryRepository categoryRepository, ImageStorageService imageStorageService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.postImageRepository = postImageRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    public Page<PostResponse> getAllPosts(String category, String sort, String status, int page, int size) {
        Sort sortOrder = switch (sort) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        PageRequest pageRequest = PageRequest.of(page, size, sortOrder);

        Page<Post> posts;
        if (category != null && !category.isEmpty()) {
            if (status != null && !status.isEmpty()) {
                PostStatus postStatus = PostStatus.valueOf(status);
                posts = postRepository.findByCategoryAndStatus(category, postStatus, pageRequest);
            } else {
                posts = postRepository.findByCategory(category, pageRequest);
            }
        } else {
            posts = postRepository.findAll(pageRequest);
        }

        List<Long> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Long> commentCountMap = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return posts.map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0L).intValue()));
    }

    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        return PostResponse.from(post, commentRepository.countByPostId(post.getId()));
    }

    public PostResponse createPost(PostRequest request, List<MultipartFile> images, User author) throws IOException {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (!categoryRepository.existsByName(request.getCategory())) {
            throw new RuntimeException("존재하지 않는 게시판입니다.");
        }
        post.setCategory(request.getCategory());
        post.setAuthor(author);

        categoryRepository.findByName(request.getCategory()).ifPresent(cat -> {
            if (cat.isHasStatus()) {
                post.setStatus(PostStatus.REGISTERED);
            }
        });

        Post saved = postRepository.save(post);
        saveImages(saved, images);
        return PostResponse.from(saved, 0);
    }

    private void saveImages(Post post, List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) return;

        int order = 0;
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) continue;
            String url = imageStorageService.upload(image);

            PostImage postImage = new PostImage(post, url, order);
            post.getImages().add(postImage);

            if (order == 0) {
                post.setImageUrl(url);
            }
            order++;
        }
        postRepository.save(post);
    }

    @Transactional
    public PostResponse toggleLike(Long id, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean alreadyLiked = postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId());
        if (alreadyLiked) {
            postLikeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            PostLike postLike = new PostLike();
            postLike.setUser(user);
            postLike.setPost(post);
            postLikeRepository.save(postLike);
            post.setLikeCount(post.getLikeCount() + 1);
        }

        postRepository.save(post);
        PostResponse response = PostResponse.from(post, commentRepository.countByPostId(post.getId()));
        response.setLiked(!alreadyLiked);
        return response;
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, List<MultipartFile> images, List<String> existingImageUrls, User user) throws IOException {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (!categoryRepository.existsByName(request.getCategory())) {
            throw new RuntimeException("존재하지 않는 게시판입니다.");
        }
        post.setCategory(request.getCategory());

        boolean hasNewImages = images != null && !images.isEmpty() && images.stream().anyMatch(f -> f != null && !f.isEmpty());
        List<String> keepUrls = existingImageUrls != null ? existingImageUrls : List.of();

        // 프론트가 보낸 유지 목록에 없는 기존 이미지만 제거
        post.getImages().removeIf(img -> !keepUrls.contains(img.getImageUrl()));

        // 유지된 이미지 순서 재정렬
        int order = 0;
        for (String url : keepUrls) {
            for (PostImage img : post.getImages()) {
                if (img.getImageUrl().equals(url)) {
                    img.setSortOrder(order++);
                    break;
                }
            }
        }

        // 새 이미지 추가
        if (hasNewImages) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) continue;
                String url = imageStorageService.upload(image);
                PostImage postImage = new PostImage(post, url, order++);
                post.getImages().add(postImage);
            }
        }

        // 썸네일 갱신
        post.setImageUrl(post.getImages().isEmpty() ? null : post.getImages().stream()
                .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(PostImage::getImageUrl).orElse(null));

        postRepository.save(post);

        Post saved = postRepository.findById(id).orElseThrow();
        return PostResponse.from(saved, commentRepository.countByPostId(saved.getId()));
    }

    @Transactional
    public PostResponse updatePostStatus(Long id, PostStatus status, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("본인이 작성한 글만 상태를 변경할 수 있습니다.");
        }

        post.setStatus(status);
        postRepository.save(post);
        return PostResponse.from(post, commentRepository.countByPostId(post.getId()));
    }

    @Transactional
    public void deletePost(Long id, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        bookmarkRepository.deleteByPostId(id);
        postLikeRepository.deleteByPostId(id);
        commentRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    public boolean isLiked(Long userId, Long postId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
}
