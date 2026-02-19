# 다중 이미지 업로드 (SPEC 6-B)

## Context
현재 게시글당 이미지 1장만 가능. 최대 5장까지 업로드하고, 첫 번째 이미지를 썸네일로, 상세 페이지에서는 캐러셀로 표시해야 함.

## 설계 방향
- `Post.imageUrl` (단일 String) → `PostImage` 엔티티 분리 (@OneToMany)
- 기존 `imageUrl` 필드는 유지하여 **썸네일 전용** (첫 번째 이미지 URL 캐싱) → GridItem/TitleCard 변경 없음
- 새 `PostImage` 테이블에 전체 이미지 목록 저장
- PostResponse에 `imageUrls: List<String>` 추가하여 상세/편집 페이지에서 사용

## 백엔드 수정 (6개 파일)

### 1. `entity/PostImage.java` (신규)
- id, post(@ManyToOne), imageUrl, sortOrder
- Post 삭제 시 cascade 삭제

### 2. `repository/PostImageRepository.java` (신규)
- `List<PostImage> findByPostIdOrderBySortOrder(Long postId)`
- `void deleteByPostId(Long postId)`

### 3. `entity/Post.java`
- `@OneToMany(mappedBy="post", cascade=ALL, orphanRemoval=true) List<PostImage> images` 추가
- 기존 `imageUrl` 필드 유지 (썸네일 역할, GridItem 호환)

### 4. `service/PostService.java`
- `createPost`: `MultipartFile image` → `List<MultipartFile> images` (최대 5장)
    - 각 이미지 저장 후 PostImage 엔티티 생성, sortOrder 부여
    - 첫 번째 이미지 URL을 `post.imageUrl`에도 세팅 (썸네일)
- `updatePost`: 동일하게 다중 이미지 처리
    - 새 이미지가 오면 기존 PostImage 전체 삭제 후 재등록

### 5. `controller/PostController.java`
- `@RequestParam("image") MultipartFile` → `@RequestParam(value="images", required=false) List<MultipartFile> images`
- createPost, updatePost 모두 변경

### 6. `dto/PostResponse.java`
- `List<String> imageUrls` 필드 추가
- `from()` 메서드에서 `post.getImages()` → URL 리스트 매핑

## 프론트엔드 수정 (4개 파일)

### 7. `types/index.ts`
- Post 인터페이스에 `imageUrls?: string[]` 추가 (기존 `imageUrl` 유지)

### 8. `app/posts/new/page.tsx`
- `imageFile` → `imageFiles: File[]` (최대 5개)
- `imagePreview` → `imagePreviews: string[]`
- 파일 입력: `multiple` 속성 추가, 5장 초과 시 alert
- 미리보기: 가로 스크롤 썸네일 리스트 + 개별 삭제(X) 버튼
- FormData: `images` 키로 각 파일 append

### 9. `app/posts/[id]/edit/page.tsx`
- 동일한 다중 이미지 UI
- 기존 이미지 로드 시 `post.imageUrls`로 미리보기 세팅

### 10. `components/PostDetail.tsx`
- 단일 `<img>` → 이미지 캐러셀 (좌우 화살표 + 인디케이터 도트)
- `post.imageUrls` 배열로 렌더링
- 이미지 1장이면 화살표 숨김

### 11. `components/GridItem.tsx`
- 변경 없음 (기존 `post.imageUrl` = 썸네일, 그대로 사용)

## 검증
1. 새 글 작성 시 이미지 0~5장 선택 → 저장 → 상세 페이지에서 캐러셀 확인
2. 6장 이상 선택 시 alert 차단
3. 메인 피드 썸네일이 첫 번째 이미지로 표시
4. 글 수정 시 기존 이미지 표시 + 새 이미지로 교체 가능
5. 이미지 없는 글은 기존처럼 타이틀카드 표시
