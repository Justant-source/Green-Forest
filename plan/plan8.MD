# 대댓글(무제한 depth) 기능 구현

## Context
현재 댓글은 1depth flat 구조만 지원. 댓글에 대한 답글(대댓글)을 무제한 depth로 작성할 수 있도록 확장한다. 시각적 들여쓰기는 3단계까지만 적용하여 UI가 지나치게 좁아지는 것을 방지한다.

## 수정 파일

### 백엔드 (5개 파일)

#### 1. `backend/src/main/java/com/vgc/entity/Comment.java`
- `parent` 필드 추가: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Comment parent;`
- `replies` 필드 추가: `@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL) @OrderBy("createdAt ASC") private List<Comment> replies = new ArrayList<>();`
- getter/setter 추가
- `@JsonIgnore`를 replies에는 붙이지 않음 (API 응답에 트리 구조로 포함)
- `parent`에는 `@JsonIgnore` 붙여서 순환참조 방지

#### 2. `backend/src/main/java/com/vgc/dto/CommentRequest.java`
- `private Long parentId;` 필드 + getter/setter 추가

#### 3. `backend/src/main/java/com/vgc/repository/CommentRepository.java`
- `findByPostIdAndParentIsNullOrderByCreatedAtDesc(Long postId)` 메서드 추가 — 루트 댓글만 조회 (대댓글은 entity의 replies로 자동 로드)
- 기존 `findByPostIdOrderByCreatedAtDesc`는 유지 (전체 카운트 등에서 사용 가능)

#### 4. `backend/src/main/java/com/vgc/service/CommentService.java`
- `getComments()`: `findByPostIdAndParentIsNullOrderByCreatedAtDesc` 사용으로 변경 → 루트 댓글 + replies 트리 반환
- `addComment()`: `request.getParentId()`가 있으면 해당 Comment를 찾아서 parent로 설정

#### 5. `backend/src/main/java/com/vgc/controller/CommentController.java`
- 변경 없음 (Service 레이어에서 처리하므로 Controller는 그대로)

### 프론트엔드 (3개 파일)

#### 6. `frontend/src/types/index.ts`
- `Comment` 타입에 `parentId: number | null;`, `replies: Comment[];` 추가

#### 7. `frontend/src/lib/api.ts`
- `addComment(postId, content, parentId?)` — parentId 파라미터 추가, body에 포함

#### 8. `frontend/src/components/CommentSection.tsx`
- 재귀 `CommentItem` 컴포넌트 추가:
    - 댓글 내용 + "답글" 버튼 렌더링
    - 답글 버튼 클릭 시 인라인 답글 입력폼 토글
    - `depth` prop으로 들여쓰기 제어 (시각적 들여쓰기 최대 3단계: `ml-0`, `ml-8`, `ml-16`, `ml-20`)
    - `comment.replies`가 있으면 재귀적으로 `CommentItem` 렌더링
- 댓글 총 개수: 루트 댓글 수가 아닌 전체 댓글 수 표시 (재귀 카운트 또는 별도 API)
- 새 대댓글 작성 후 로컬 state에 즉시 반영

### DB 변경
- `comments` 테이블에 `parent_id BIGINT NULL` 컬럼 추가
- JPA auto-ddl로 자동 처리 (spring.jpa.hibernate.ddl-auto 설정에 따라), 아니면 수동 ALTER

## 주의사항
- `countByPostId` / `countByPostIdIn` — 대댓글도 같은 `post_id`를 가지므로 전체 댓글수 집계에 영향 없음
- `deleteByPostId` — 대댓글도 같은 `post_id`이므로 게시글 삭제 시 전체 삭제됨
- 순환참조 방지: `parent` 필드에 `@JsonIgnore`, `replies`는 직렬화 허용

## 검증
- 댓글 작성 → 해당 댓글에 답글 작성 → 답글의 답글 작성 → 트리 구조로 표시되는지 확인

