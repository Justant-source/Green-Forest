---
title: L1 Context — frontend 경계
last_updated: 2026-08-31
---

# frontend/10-context — Next.js UI

> 브라우저 클라이언트. REST는 백엔드만 호출한다. LLM 직접 호출 없음.

---

## §1. 경계

| 항목 | 값 |
|---|---|
| 프레임워크 | Next.js App Router (`frontend/src/app/`) |
| 스타일 | Tailwind CSS (`frontend/tailwind.config.ts`) |
| 상태 | React Context 3개 |
| TS/TSX | `frontend/src` 아래 **92** 파일 (2026-08-31) |

---

## §2. Context

| Context | 파일 | 역할 |
|---|---|---|
| Auth | `frontend/src/context/AuthContext.tsx` | 로그인 세션 |
| Category | `frontend/src/context/CategoryContext.tsx` | 피드 카테고리 필터 |
| EventMode | `frontend/src/context/EventModeContext.tsx` | 이벤트 모드 배너 |

마운트: `frontend/src/app/layout.tsx`.

---

## §3. 빌드 · 환경변수

| 환경 | 동작 |
|---|---|
| dev | `next dev`, `env_file: ./frontend/.env.dev`, 컨테이너 restart로 `NEXT_PUBLIC_*` 반영 |
| prod | `next build` 시점에 `NEXT_PUBLIC_*` 번들 (`docker-compose.prod.yml` build args) |

파일: `frontend/.env.dev` (gitignore), `frontend/.env.production` (git 추적 — 빌드 필수, 삭제 금지).

WebSocket: `frontend/src/lib/websocket.ts` + `frontend/src/components/ChatRoom.tsx`.
