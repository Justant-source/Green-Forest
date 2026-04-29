package com.vgc.service;

import com.vgc.dto.PostRequest;
import com.vgc.dto.PostResponse;
import com.vgc.entity.Post;
import com.vgc.entity.PostImage;
import com.vgc.entity.PostLike;
import com.vgc.entity.PostStatus;
import com.vgc.entity.PostTag;
import com.vgc.entity.User;
import com.vgc.repository.BookmarkRepository;
import com.vgc.repository.CategoryRepository;
import com.vgc.repository.CommentRepository;
import com.vgc.repository.PostImageRepository;
import com.vgc.repository.PostLikeRepository;
import com.vgc.repository.PostRepository;
import com.vgc.repository.PostTagRepository;
import com.vgc.repository.QuestCompletionRepository;
import com.vgc.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
    private final DropService dropService;
    private final UserRepository userRepository;
    private final PostTagRepository postTagRepository;
    private final QuestCompletionRepository questCompletionRepository;
    private final PlantGrowthService plantGrowthService;
    private final ActivityLogService activityLogService;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository, PostImageRepository postImageRepository,
                       BookmarkRepository bookmarkRepository, CategoryRepository categoryRepository,
                       ImageStorageService imageStorageService, DropService dropService,
                       UserRepository userRepository, PostTagRepository postTagRepository,
                       QuestCompletionRepository questCompletionRepository, PlantGrowthService plantGrowthService,
                       ActivityLogService activityLogService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.postImageRepository = postImageRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.dropService = dropService;
        this.userRepository = userRepository;
        this.postTagRepository = postTagRepository;
        this.questCompletionRepository = questCompletionRepository;
        this.plantGrowthService = plantGrowthService;
        this.activityLogService = activityLogService;
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

        return posts.map(post -> {
            PostResponse response = PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0L).intValue());
            var categoryEntity = categoryRepository.findByName(post.getCategory());
            if (categoryEntity.isPresent()) {
                response.setCategoryHasStatus(categoryEntity.get().isHasStatus());
            }
            return response;
        });
    }

    public PostResponse getPost(Long id, String currentUserEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        PostResponse response = PostResponse.from(post, commentRepository.countByPostId(post.getId()));
        if (currentUserEmail != null && post.getAuthor() != null) {
            response.setIsAuthor(currentUserEmail.equals(post.getAuthor().getEmail()));
        }

        // categoryHasStatus 필드 추가
        var category = categoryRepository.findByName(post.getCategory());
        if (category.isPresent()) {
            response.setCategoryHasStatus(category.get().isHasStatus());
        }

        List<PostTag> tags = postTagRepository.findByPostId(id);
        if (!tags.isEmpty()) {
            response.setTaggedNicknames(
                tags.stream().map(t -> t.getTaggedUser().getName() + "(" + t.getTaggedUser().getNickname() + ")").collect(Collectors.toList())
            );
        }

        return response;
    }

    @Transactional
    public PostResponse createPost(PostRequest request, List<MultipartFile> images, User author) throws IOException {
        // 카테고리 검증 (긍정문구, 동료칭찬, 퀘스트만 허용; survey는 SurveyController 전용)
        String category = request.getCategory();
        if ("survey".equals(category)) {
            throw new RuntimeException("설문 카테고리는 관리자 전용 API로만 작성할 수 있습니다.");
        }
        if (!List.of("긍정문구", "동료칭찬", "퀘스트").contains(category)) {
            throw new RuntimeException("유효하지 않은 카테고리입니다. (긍정문구/동료칭찬/퀘스트)");
        }

        // 동료칭찬 주 1회 제한
        if ("동료칭찬".equals(category) && dropService.hasPraisedThisWeek(author.getId())) {
            LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            throw new RuntimeException(String.format(
                "동료 칭찬은 주 1회만 작성할 수 있습니다. 다음 작성은 %d월 %d일(월요일)부터 가능합니다.",
                nextMonday.getMonthValue(), nextMonday.getDayOfMonth()));
        }

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(category);
        post.setAuthor(author);
        post.setQuestId(request.getQuestId());
        post.setAnonymous(request.isAnonymous());

        Post saved = postRepository.save(post);
        saveImages(saved, images);

        // 태깅된 유저 조회
        List<User> taggedUsers = new ArrayList<>();
        if (request.getTaggedNicknames() != null && !request.getTaggedNicknames().isEmpty()) {
            taggedUsers = userRepository.findByNicknameIn(request.getTaggedNicknames());
        }

        // 물방울 자동 지급 (핵심 로직)
        int dropsAwarded = dropService.awardDropsForPost(author, saved, category, taggedUsers);
        plantGrowthService.onPostCreated(
            author.getId(),
            saved.getId(),
            saved.getContent() != null ? saved.getContent().length() : 0
        );

        activityLogService.logPostCreate(author.getId(), author.getNickname(), category, saved.getId());

        PostResponse response = PostResponse.from(saved, 0);
        response.setDropsAwarded(dropsAwarded);

        var categoryEntity = categoryRepository.findByName(category);
        if (categoryEntity.isPresent()) {
            response.setCategoryHasStatus(categoryEntity.get().isHasStatus());
        }

        if (!taggedUsers.isEmpty()) {
            response.setTaggedNicknames(
                taggedUsers.stream().map(u -> u.getName() + "(" + u.getNickname() + ")").collect(Collectors.toList())
            );
        }

        return response;
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
            if (post.getAuthor() != null) {
                plantGrowthService.onLikeRemoved(post.getAuthor().getId(), post.getId());
            }
        } else {
            PostLike postLike = new PostLike();
            postLike.setUser(user);
            postLike.setPost(post);
            postLikeRepository.save(postLike);
            post.setLikeCount(post.getLikeCount() + 1);
            dropService.awardDropsForLike(user, post);
            if (post.getAuthor() != null) {
                plantGrowthService.onLikeReceived(post.getAuthor().getId(), post.getId());
            }
        }

        postRepository.save(post);
        PostResponse response = PostResponse.from(post, commentRepository.countByPostId(post.getId()));
        response.setLiked(!alreadyLiked);

        var category = categoryRepository.findByName(post.getCategory());
        if (category.isPresent()) {
            response.setCategoryHasStatus(category.get().isHasStatus());
        }

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
        post.setCategory(request.getCategory());

        boolean hasNewImages = images != null && !images.isEmpty() && images.stream().anyMatch(f -> f != null && !f.isEmpty());
        List<String> keepUrls = existingImageUrls != null ? existingImageUrls : List.of();

        post.getImages().removeIf(img -> !keepUrls.contains(img.getImageUrl()));

        int order = 0;
        for (String url : keepUrls) {
            for (PostImage img : post.getImages()) {
                if (img.getImageUrl().equals(url)) {
                    img.setSortOrder(order++);
                    break;
                }
            }
        }

        if (hasNewImages) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) continue;
                String url = imageStorageService.upload(image);
                PostImage postImage = new PostImage(post, url, order++);
                post.getImages().add(postImage);
            }
        }

        post.setImageUrl(post.getImages().isEmpty() ? null : post.getImages().stream()
                .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(PostImage::getImageUrl).orElse(null));

        postRepository.save(post);

        // 태그 diff 처리
        List<PostTag> existingTags = postTagRepository.findByPostId(id);
        List<Long> existingTaggedUserIds = existingTags.stream()
                .map(pt -> pt.getTaggedUser().getId())
                .toList();

        List<String> newNicknames = request.getTaggedNicknames() != null ? request.getTaggedNicknames() : List.of();
        List<User> newTaggedUsers = newNicknames.isEmpty() ? List.of() : userRepository.findByNicknameIn(newNicknames);
        List<Long> newTaggedUserIds = newTaggedUsers.stream().map(User::getId).toList();

        // 제거된 태그 → 물방울 회수
        for (PostTag existing : existingTags) {
            if (!newTaggedUserIds.contains(existing.getTaggedUser().getId())) {
                dropService.revokeTagBonus(existing.getTaggedUser(), post);
            }
        }

        // 추가된 태그 → 물방울 지급
        for (User newUser : newTaggedUsers) {
            if (!existingTaggedUserIds.contains(newUser.getId())) {
                dropService.awardNewTagBonus(post.getAuthor(), post, newUser);
            }
        }

        activityLogService.logPostUpdate(user.getId(), user.getNickname(), id);

        Post saved = postRepository.findById(id).orElseThrow();
        List<PostTag> updatedTags = postTagRepository.findByPostId(id);
        PostResponse response = PostResponse.from(saved, commentRepository.countByPostId(saved.getId()));

        var category = categoryRepository.findByName(saved.getCategory());
        if (category.isPresent()) {
            response.setCategoryHasStatus(category.get().isHasStatus());
        }

        if (!updatedTags.isEmpty()) {
            response.setTaggedNicknames(
                updatedTags.stream()
                    .map(pt -> pt.getTaggedUser().getName() + "(" + pt.getTaggedUser().getNickname() + ")")
                    .toList()
            );
        }
        return response;
    }

    @Transactional
    public PostResponse updatePostStatus(Long id, PostStatus status, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("본인이 작성한 글만 상태를 변경할 수 있습니다.");
        }

        var category = categoryRepository.findByName(post.getCategory());
        if (!category.isPresent() || !category.get().isHasStatus()) {
            throw new RuntimeException("이 카테고리는 상태 변경을 지원하지 않습니다.");
        }

        post.setStatus(status);
        postRepository.save(post);
        PostResponse response = PostResponse.from(post, commentRepository.countByPostId(post.getId()));
        response.setCategoryHasStatus(category.get().isHasStatus());
        return response;
    }

    @Transactional
    public void deletePost(Long id, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        activityLogService.logPostDelete(user.getId(), user.getNickname(), id);
        questCompletionRepository.deleteByPostId(id);
        postTagRepository.deleteByPostId(id);
        bookmarkRepository.deleteByPostId(id);
        postLikeRepository.deleteByPostId(id);
        commentRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    public boolean isLiked(Long userId, Long postId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
}
