---
title: L3 — 페이지 · Context · 컴포넌트
last_updated: 2026-09-08
---

# frontend/30-components/component-tree

> `page.tsx` 목록은 아래 표가 정본. 요청 탭 라우트가 추가됐다.

---

## §1. App Router 페이지

| 경로 | 파일 |
|---|---|
| `/` | `frontend/src/app/page.tsx` |
| `/login` | `frontend/src/app/login/page.tsx` |
| `/register` | `frontend/src/app/register/page.tsx` |
| `/posts/new` | `frontend/src/app/posts/new/page.tsx` |
| `/posts/[id]` | `frontend/src/app/posts/[id]/page.tsx` |
| `/posts/[id]/edit` | `frontend/src/app/posts/[id]/edit/page.tsx` |
| `/posts/[id]/survey-edit` | `frontend/src/app/posts/[id]/survey-edit/page.tsx` |
| `/admin` | `frontend/src/app/admin/page.tsx` |
| `/events` | `frontend/src/app/events/page.tsx` |
| `/events/[id]` | `frontend/src/app/events/[id]/page.tsx` |
| `/attendance` | `frontend/src/app/attendance/page.tsx` |
| `/attendance/me` | `frontend/src/app/attendance/me/page.tsx` |
| `/gacha` | `frontend/src/app/gacha/page.tsx` |
| `/gacha/me` | `frontend/src/app/gacha/me/page.tsx` |
| `/quests` | `frontend/src/app/quests/page.tsx` |
| `/garden` | `frontend/src/app/garden/page.tsx` |
| `/ranking` | `frontend/src/app/ranking/page.tsx` |
| `/conversations` | `frontend/src/app/conversations/page.tsx` |
| `/conversations/[id]` | `frontend/src/app/conversations/[id]/page.tsx` |
| `/notifications` | `frontend/src/app/notifications/page.tsx` |
| `/profile` | `frontend/src/app/profile/page.tsx` |
| `/profile/birthday` | `frontend/src/app/profile/birthday/page.tsx` |
| `/profile/shipping` | `frontend/src/app/profile/shipping/page.tsx` |
| `/requests` | `frontend/src/app/requests/page.tsx` (→ `/requests/snacks`) |
| `/requests/snacks` | `frontend/src/app/requests/snacks/page.tsx` |
| `/requests/supplies` | `frontend/src/app/requests/supplies/page.tsx` |

레이아웃: `frontend/src/app/layout.tsx`. 요청 셸: `frontend/src/app/requests/layout.tsx`.

`BottomNav` 로그인 후: 광장 / 출석 / 뽑기 / 요청(`/requests/snacks`) / 마이가든. `/ranking`은 하단에서 제거하고 마이가든 드롭 탭 링크만 둔다.

---

## §2. Context

`AuthContext` · `CategoryContext` · `EventModeContext` — 경로와 역할은 `docs/frontend/10-context.md`.

---

## §3. 컴포넌트 분류

| 종류 | 예 |
|---|---|
| 셸 | `Header.tsx`, `BottomNav.tsx`, `GridFeed.tsx`, `GridItem.tsx`, `TitleCard.tsx` |
| 게시·설문 | `PostContent.tsx`, `PostDetail.tsx`, `CommentSection.tsx`, `SurveyView.tsx`, `SurveyCreateForm.tsx` |
| 기능 | `GachaDrawModal.tsx`, `ChatRoom.tsx`, `AttendanceBoard.tsx`, `PlantGrowthBadge.tsx` |
| 이벤트 | `frontend/src/components/events/` (photobingo, photoexhibition, `EventModeBanner.tsx`) |
| 관리 | `frontend/src/components/admin/DropHistoryPanel.tsx`, `RequestAdminPanel.tsx` |
| 요청 | `frontend/src/components/requests/SnackCard.tsx`, `MyRequestList.tsx`, `SnackCategoryTabs.tsx` |

`/requests/snacks`는 보이는 상품이 있는 SSG 분류 탭만 렌더한다. 관리자 카탈로그는 12분류를 모두 보여 숨김을 설정한다. 관리자 요청 탭은 **대기 / 이력**으로 구매·거절 기록을 본다.

API 클라이언트: `frontend/src/lib/api.ts`, `frontend/src/lib/events/api.ts`.
