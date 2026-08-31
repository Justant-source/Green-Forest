---
title: L7 Policy — frontend 환경변수 · HMR
last_updated: 2026-08-31
---

# frontend/70-policy

---

## §1. NEXT_PUBLIC_* 주입 시점

| 환경 | 주입 | 값 변경 시 |
|---|---|---|
| dev | 런타임 (`env_file: ./frontend/.env.dev`) | 컨테이너 `restart` |
| prod | 빌드 타임 (`docker-compose.prod.yml` `args`) | `up -d --build frontend` |

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev restart frontend
docker compose -p green-forest-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build frontend
```

`frontend/.env.production`은 git 추적한다. 지우면 prod 빌드가 깨진다.

---

## §2. Cloudflare URL 필수 (IP 금지)

`NEXT_PUBLIC_API_BASE_URL`은 도메인만.

| 환경 | 값 |
|---|---|
| dev | `https://dev.green-office.uk/api` |
| prod | `https://green-office.uk/api` |

사고 2026-04-21: 서버 IP 변경 후 `.env.dev` 미갱신 → 브라우저 API 전부 실패.

WebSocket도 동일하게 공개 도메인을 쓴다 (`frontend/src/lib/websocket.ts`의 `NEXT_PUBLIC_WS_URL`).

---

## §3. Hot reload

`frontend/src/**` 저장 → `next dev` HMR. 의존성/`Dockerfile.dev`가 아니면 `--build` 하지 않는다.

Tailwind 유틸 클래스만 사용한다 (`frontend/src/app/globals.css`, `frontend/tailwind.config.ts`). 인라인 대형 CSS 시트를 새로 만들지 않는다.
