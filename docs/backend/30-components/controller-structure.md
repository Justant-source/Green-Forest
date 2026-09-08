---
title: L3 — 컨트롤러 분류 · HTTP 매핑
last_updated: 2026-09-08
---

# backend/30-components/controller-structure

> `@RestController` / `@Controller` 개수와 HTTP 매핑 합계는 **코드가 정본**. 2026-09-08 `*Controller.java` 실측 32파일, HTTP 매핑 **197**.
> 계획서 스냅샷의 199는 코드와 불일치 — **코드가 정본**.

---

## §1. 매핑 분포

| 메서드 | 건수 |
|---|---|
| GET | 92 |
| POST | 50 |
| PUT | 19 |
| DELETE | 18 |
| PATCH | 18 |
| **합계** | **197** |

`ChatMessageController`는 HTTP 매핑이 없고 STOMP `@MessageMapping`만 쓴다.

---

## §2. 패키지별 컨트롤러

<!-- last-verified: 2026-09-08 -->
<!-- code-ref: backend/src/main/java/com/vgc/controller/**/*.java -->

```mermaid
flowchart TB
  subgraph api["/api 공개·회원"]
    AuthController
    UserController
    PostController
    CommentController
    BookmarkController
    CategoryController
    ProfileController
    NotificationController
    ConversationController
    QuestController
    PlantGrowthController
    LeaderboardController
    PlazaController
    SurveyController
    AttendanceController
    GachaController
    AnnouncementController
    WeeklyReportController
    FileServeController
    BotController
    NotifyEventController
    RequestController
  end
  subgraph admin["/api/admin"]
    AdminController
    AdminDropController
    EventAdminController
    PhotoBingoAdminController
    PhotoExhibitionAdminController
    AdminRequestController
  end
  subgraph event["/api/events"]
    EventController
    PhotoBingoController
    PhotoExhibitionController
  end
  subgraph ws["STOMP"]
    ChatMessageController
  end
```

---

## §3. RequestMapping 목록

| 클래스 | prefix |
|---|---|
| `AuthController` | `/api/auth` |
| `UserController` | `/api/users` |
| `PostController` | `/api/posts` |
| `CommentController` | `/api/posts/{postId}/comments` |
| `BookmarkController` | `/api/posts` |
| `CategoryController` | `/api/categories` |
| `ProfileController` | `/api/profile` |
| `NotificationController` | `/api/notifications` |
| `ConversationController` | `/api/conversations` |
| `QuestController` | `/api/quests` |
| `PlantGrowthController` | `/api/plant` |
| `LeaderboardController` | `/api/leaderboard` |
| `PlazaController` | `/api/plaza` |
| `SurveyController` | `/api/surveys` |
| `AttendanceController` | `/api/attendance` |
| `GachaController` | `/api/gacha` |
| `AnnouncementController` | `/api/announcements` |
| `WeeklyReportController` | `/api/weekly-reports` |
| `FileServeController` | `/api/media` |
| `BotController` | `/api/bot` |
| `NotifyEventController` | `/api/notify` |
| `RequestController` | `/api/requests` |
| `AdminController` | `/api/admin` |
| `AdminDropController` | `/api/admin/drop-transactions` |
| `EventAdminController` | `/api/admin/events` |
| `PhotoBingoAdminController` | `/api/admin/events/{eventId}` |
| `PhotoExhibitionAdminController` | `/api/admin/events/{eventId}/photo-exhibition` |
| `AdminRequestController` | `/api/admin/requests` |
| `EventController` | `/api/events` |
| `PhotoBingoController` | `/api/events/{eventId}/photo-bingo` |
| `PhotoExhibitionController` | `/api/events/{eventId}/photo-exhibition` |
| `ChatMessageController` | (없음) `/app/chat/{conversationId}` |

경로 권위: 각 파일의 `@RequestMapping`.

`RequestController` 카탈로그: `GET /api/requests/snacks/categories`(사용자에게 **보이는 상품이 있는 분류만**), `GET /api/requests/snacks/catalog?category=`.
`AdminRequestController` 카탈로그: `GET /api/admin/requests/catalog/categories`(12분류 전부), `GET /api/admin/requests/catalog?category=`, `PATCH /api/admin/requests/catalog/{id}/visibility`, `PATCH /api/admin/requests/catalog/categories/{category}/visibility`, `POST /api/admin/requests/catalog/sync`(관리자 버튼만, 쿨다운 없음).
이력: `GET /api/admin/requests/snacks/history`, `GET /api/admin/requests/supplies/history`.

---

## §4. NotifyTokenFilter

`/api/notify/**`만 검사한다. Bearer 토큰을 timing-safe 비교하고 `ROLE_NOTIFY_POLLER`를 심는다.

코드: `backend/src/main/java/com/vgc/security/NotifyTokenFilter.java`
등록: `backend/src/main/java/com/vgc/config/SecurityConfig.java`
