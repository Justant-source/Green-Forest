---
title: L3 — 페이지 · Context · 컴포넌트
last_updated: 2026-08-31
---

# frontend/30-components/component-tree

> `frontend/src` TS/TSX 92파일, `frontend/src/components` TSX 47파일, `page.tsx` 23개 (2026-08-31).

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

레이아웃: `frontend/src/app/layout.tsx`.

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
| 관리 | `frontend/src/components/admin/DropHistoryPanel.tsx` |

API 클라이언트: `frontend/src/lib/api.ts`, `frontend/src/lib/events/api.ts`.
