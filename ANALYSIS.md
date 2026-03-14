# 그린 포레스트 - Phase 0 분석 결과서

> 원본: [dalkong10300/vgc](https://github.com/dalkong10300/vgc) (냐옹스 - 고양이 커뮤니티)
> 하드포크: [dalkong10300/green-forest](https://github.com/dalkong10300/green-forest)
> 분석일: 2026-03-13

---

## 1. 기술 스택

| 레이어 | 기술 | 버전 |
|--------|------|------|
| **프론트엔드** | Next.js (App Router) | 14.2.x |
| **UI** | React + TypeScript | 18.3 / 5.9 |
| **스타일링** | Tailwind CSS | 3.4.x |
| **백엔드** | Spring Boot | 3.2.5 |
| **언어 (BE)** | Java | 17 (소스) / 21 (빌드) |
| **빌드 도구** | Gradle | Wrapper 포함 |
| **DB** | MySQL | 8.0 |
| **ORM** | Spring Data JPA / Hibernate | (번들) |
| **인증** | JWT (JJWT 0.12.5) + Spring Security 6 | 토큰 만료 30일 |
| **실시간** | WebSocket + STOMP | SockJS fallback |
| **파일 저장** | AWS S3 (운영) / 로컬 파일시스템 (개발) | |
| **컨테이너** | Docker + Docker Compose | Nginx 리버스 프록시 |

---

## 2. 프로젝트 구조

```
green-forest/
├── CLAUDE.md                  # AI 개발 가이드
├── SPEC.md                    # 제품 스펙 (원본)
├── docker-compose.yml         # 멀티 서비스 오케스트레이션
├── nginx/
│   └── nginx.conf             # 리버스 프록시 설정
├── frontend/                  # Next.js TypeScript
│   ├── package.json
│   ├── next.config.js
│   ├── tailwind.config.ts
│   └── src/
│       ├── app/               # App Router 페이지
│       │   ├── layout.tsx     # 루트 레이아웃 (AuthProvider + Header)
│       │   ├── page.tsx       # 홈 (GridFeed)
│       │   ├── login/
│       │   ├── register/
│       │   ├── posts/
│       │   │   ├── new/
│       │   │   └── [id]/
│       │   │       └── edit/
│       │   ├── profile/
│       │   ├── conversations/
│       │   │   └── [id]/
│       │   └── admin/
│       ├── components/        # 12개 재사용 컴포넌트
│       ├── context/           # AuthContext
│       ├── lib/               # API, auth, websocket, utils
│       └── types/             # TypeScript 인터페이스
└── backend/                   # Spring Boot Java
    ├── build.gradle
    └── src/main/java/com/vgc/
        ├── config/            # Security, WebSocket, FileStorage, DataInitializer
        ├── security/          # JwtUtil, JwtAuthenticationFilter
        ├── controller/        # 9개 REST 컨트롤러
        ├── service/           # 7개 서비스 클래스
        ├── repository/        # 10개 JPA 리포지토리
        ├── entity/            # 10개 JPA 엔티티 + 1 enum
        └── dto/               # 11개 DTO
```

---

## 3. 기존 DB 엔티티 (10개 + 1 enum)

### 3.1 users
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| email | String | NOT NULL, UNIQUE |
| password | String | NOT NULL (BCrypt) |
| nickname | String | NOT NULL |
| role | String | default "USER" ("USER" / "ADMIN") |
| createdAt | LocalDateTime | auto |

### 3.2 posts
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| title | String | NOT NULL |
| content | TEXT | |
| imageUrl | String | nullable (썸네일) |
| category | String | NOT NULL |
| author_id | Long (FK→users) | ManyToOne LAZY |
| status | enum | nullable (REGISTERED/ING/COMPLETE) |
| likeCount | int | default 0 |
| viewCount | int | default 0 |
| createdAt | LocalDateTime | auto |

인덱스: `(category, status)`, `(author_id, createdAt DESC)`, `(createdAt DESC)`

### 3.3 post_images
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| post_id | Long (FK→posts) | cascade ALL |
| imageUrl | String | NOT NULL |
| sortOrder | int | 정렬 순서 |

### 3.4 post_likes
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| user_id | Long (FK) | UNIQUE(user_id, post_id) |
| post_id | Long (FK) | |
| createdAt | LocalDateTime | auto |

### 3.5 comments (중첩 댓글, 소프트 삭제)
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| post_id | Long (FK) | NOT NULL |
| content | String | NOT NULL |
| authorName | String | NOT NULL (작성 시점 닉네임 스냅샷) |
| author_id | Long (FK) | |
| parent_id | Long (FK→self) | nullable |
| deleted | boolean | default false |
| createdAt | LocalDateTime | auto |
| updatedAt | LocalDateTime | auto |

### 3.6 categories (동적 게시판)
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| name | String | UNIQUE, NOT NULL (슬러그) |
| label | String | NOT NULL (표시명) |
| color | String | nullable |
| hasStatus | boolean | default false |
| createdAt | LocalDateTime | auto |

### 3.7 category_requests
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| name/label/color | String | |
| status | String | PENDING/APPROVED/REJECTED |
| requester_id | Long (FK) | |
| rejectionReason | String | nullable |

### 3.8 bookmarks
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| user_id | Long (FK) | UNIQUE(user_id, post_id) |
| post_id | Long (FK) | |
| createdAt | LocalDateTime | auto |

### 3.9 conversations
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| user1_id / user2_id | Long (FK) | UNIQUE(user1_id, user2_id) |
| user1Left / user2Left | boolean | default false |
| createdAt / updatedAt | LocalDateTime | auto |

### 3.10 messages
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | Long (PK) | auto-increment |
| conversation_id | Long (FK) | |
| sender_id | Long (FK) | nullable (시스템 메시지) |
| content | String(2000) | |
| systemMessage | boolean | default false |
| createdAt | LocalDateTime | auto |

---

## 4. 기존 API 엔드포인트

### 인증
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/auth/register` | No | 회원가입 → {token, nickname, role} |
| POST | `/api/auth/login` | No | 로그인 → {token, nickname, role} |

### 게시글
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/posts?category=&sort=&status=&page=&size=` | No | 목록 (페이지네이션) |
| GET | `/api/posts/{id}` | No | 상세 (조회수 +1) |
| POST | `/api/posts` | Yes | 생성 (multipart) |
| PUT | `/api/posts/{id}` | Yes | 수정 (본인만) |
| DELETE | `/api/posts/{id}` | Yes | 삭제 (본인만) |
| PATCH | `/api/posts/{id}/status` | Yes | 상태 변경 |
| GET/POST | `/api/posts/{id}/like` | Yes | 좋아요 조회/토글 |
| GET/POST | `/api/posts/{postId}/bookmark` | Yes | 북마크 조회/토글 |

### 댓글
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/posts/{postId}/comments` | No | 댓글 트리 |
| GET | `/api/posts/{postId}/comments/count` | No | 댓글 수 |
| POST | `/api/posts/{postId}/comments` | Yes | 댓글 작성 |
| PUT/DELETE | `/api/posts/{postId}/comments/{id}` | Yes | 수정/삭제 (본인만) |

### 프로필
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/profile/posts?page=&size=` | Yes | 내 글 |
| GET | `/api/profile/bookmarks?page=&size=` | Yes | 내 북마크 |

### 카테고리
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/categories` | No | 카테고리 목록 |
| POST | `/api/categories/request` | Yes | 카테고리 요청 |

### DM/대화
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/conversations` | Yes | 대화 시작 |
| GET | `/api/conversations` | Yes | 대화 목록 |
| GET | `/api/conversations/{id}/messages` | Yes | 메시지 내역 |
| POST | `/api/conversations/{id}/messages` | Yes | 메시지 전송 + WebSocket |
| POST | `/api/conversations/{id}/leave` | Yes | 대화 나가기 |

### 관리자
| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET/POST/DELETE | `/api/admin/categories` | Admin | 카테고리 CRUD |
| GET | `/api/admin/category-requests` | Admin | 카테고리 요청 목록 |
| POST | `/api/admin/category-requests/{id}/approve` | Admin | 승인 |
| POST | `/api/admin/category-requests/{id}/reject` | Admin | 거절 |

---

## 5. 프론트엔드 라우팅 및 컴포넌트

### 라우팅
| Route | 컴포넌트 | 설명 |
|-------|----------|------|
| `/` | GridFeed | 홈 (비주얼 그리드 피드) |
| `/login` | Login | 이메일/비밀번호 로그인 |
| `/register` | Register | 회원가입 |
| `/posts/new` | NewPost | 글 작성 |
| `/posts/[id]` | PostDetail | 글 상세 |
| `/posts/[id]/edit` | EditPost | 글 수정 |
| `/profile` | Profile | 마이 페이지 (내 글/북마크) |
| `/conversations` | ConversationList | DM 목록 |
| `/conversations/[id]` | ChatRoom | 1:1 채팅 |
| `/admin` | Admin | 관리자 (카테고리 관리) |

### 주요 컴포넌트
- `Header` — 상단 네비게이션 (로고, 관리자, 새 글, 대화, 프로필)
- `GridFeed` — 무한 스크롤 그리드 + 카테고리 필터 + 정렬
- `GridItem` — 개별 그리드 카드 (이미지 or TitleCard)
- `TitleCard` — 이미지 없는 글의 컬러 카드
- `PostDetail` — 글 상세 (이미지 캐러셀, 댓글, 좋아요/북마크)
- `CommentSection` — 중첩 댓글 시스템
- `ChatRoom` — WebSocket 실시간 채팅
- `CategoryFilter` — 가로 스크롤 카테고리 필터

### 현재 색상 체계 (변경 대상)
- **Primary**: Orange (`bg-orange-600`, `bg-orange-400`)
- **배경**: `bg-orange-50/40`
- **좋아요**: Rose (`bg-rose-50 text-rose-600`)
- **로고 폰트**: Jua (한국어 라운드 서체)

---

## 6. 전환 작업 맵 (VGC → 그린 포레스트)

### 유지 (Keep)
- [x] JWT 인증 시스템 (회원가입/로그인)
- [x] 게시글 CRUD + 이미지 업로드 (S3/로컬)
- [x] 중첩 댓글 시스템
- [x] 1:1 DM (WebSocket)
- [x] Docker 배포 구성

### 수정 (Modify)
- [x] `User` 엔티티 → plant_type, job_class, element, party_id, total_drops 등 8개 필드 추가
- [x] `Post` 엔티티 → category ENUM(긍정문구/동료칭찬/퀘스트), quest_id, is_anonymous 추가
- [x] 카테고리 시스템 → 동적 카테고리에서 고정 3개 카테고리(ENUM)로 변경
- [x] 탭 네비게이션 → 5탭 (숲의 광장/긍정문구/동료칭찬/퀘스트/랭킹) + 마이가든
- [x] 글 작성 폼 → 카테고리 선택(필수), @태깅, 익명 토글 추가
- [x] 프로필 → 마이 가든 (식물 정보, 직업군, 물방울 잔고)
- [x] UI 테마 → Orange → Green (#2D8A4E 기반)
- [x] 앱 이름/로고 → 냐옹스 → 그린 포레스트

### 삭제 (Remove)
- [x] 고양이 관련 UI 에셋, 텍스트, 아이콘

### 신규 개발 (Build New)
- [x] **물방울 포인트 시스템** (자동 지급, 중복 제한, 트랜잭션 로그, 난이도 가중치)
- [x] **파티 시스템** (parties 테이블, 파티원 배정, 파티 합산)
- [x] **리더보드** (파티별/개인별 랭킹, 기간 필터)
- [x] **퀘스트 시스템** (GM 생성, 유저 인증, 자동 보상)
- [x] **태깅 시스템** (@멘션 → 물방울 5 자동 지급)
- [x] **익명 작성** (닉네임 → "익명의 그린메이커")
- [x] **GM 관리 패널 확장** (물방울 수동 지급/차감, 유저/파티 관리, 통계)
- [x] **알림 시스템** (태깅/댓글/물방울/퀘스트 알림)
- [x] **투표 기능** (퀘스트 연동)
- [ ] **보상 상점** (물방울 교환)

### 신규 DB 테이블 (8개)
1. `parties` — 파티
2. `post_tags` — 게시글 내 태깅
3. `drop_transactions` — 물방울 거래 내역
4. `quest_completion_log` — 일일/주간 중복 방지 로그
5. `quests` — GM 생성 퀘스트
6. `quest_completions` — 퀘스트 완료 기록
7. `votes` — 투표
8. `notifications` — 알림

---

## 7. 인증 흐름 (변경 불필요)

```
[프론트] POST /api/auth/login {email, password}
    → [백엔드] BCrypt 검증 → JWT 생성 (30일 만료, HS256)
    → [프론트] localStorage에 token/nickname/role 저장
    → 이후 모든 요청: Authorization: Bearer <token>

[WebSocket] STOMP CONNECT + Authorization 헤더
    → JWT 검증 → Principal 설정 → /topic/messages/{id} 구독
```

---

## 8. 환경 설정

### 백엔드
- **로컬**: MySQL localhost:3306/vgc_db, 로컬 파일 업로드, SQL 로깅 ON
- **운영**: MySQL mysql:3306/vgc_db (Docker), S3 업로드, 환경변수로 시크릿 관리

### 프론트엔드
- **로컬**: API `http://localhost:8080/api`, 이미지 `http://localhost:8080`
- **운영**: 환경변수로 설정

### Docker Compose
1. `mysql` — MySQL 8.0 + 영속 볼륨
2. `backend` — JDK 21 멀티스테이지 빌드
3. `frontend` — Node 20 Alpine
4. `nginx` — 리버스 프록시 (80번 포트)

---

## 9. DataInitializer 현황

`DataInitializer.java`의 `run()` 메서드가 **비어 있음**. 시드 데이터 없음.
→ 그린 포레스트용 시드 데이터 (파티 5개, GM 계정, 고정 카테고리 3개) 추가 필요.

---

## 10. 작업량 추정

| 구분 | 비율 | 설명 |
|------|------|------|
| 재사용 | ~30% | 인증, 기본 CRUD, DM, 파일 업로드, Docker |
| 수정 | ~25% | 엔티티 확장, UI 테마, 탭 구조, 글 작성 폼 |
| 신규 개발 | ~45% | 물방울 시스템, 리더보드, 퀘스트, 파티, 관리패널, 알림 |
