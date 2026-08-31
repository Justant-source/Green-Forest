---
title: L6 Runtime — 스케줄 · WebSocket · 배포 루프
last_updated: 2026-08-31
---

# backend/60-runtime — 자동화 작업 · WebSocket · 운영

> `@Scheduled`는 **5개** (zone=`Asia/Seoul`). 계획서의 7개는 코드와 불일치.
> `/api/health` 엔드포인트는 **없다.**

---

## §1. @Scheduled 작업

`@EnableScheduling`: `backend/src/main/java/com/vgc/VgcApplication.java`  
전용 풀: `backend/src/main/java/com/vgc/config/SchedulingConfig.java` (`taskScheduler`, pool=1)

| 클래스 | cron | 목적 |
|---|---|---|
| `AttendanceScheduler` | `0 0 11 * * MON-FRI` | 평일 11:00 출석 추첨 |
| `EventScheduler` | `0 * * * * *` | 매 분 이벤트 시작/종료 |
| `BirthdayAnnouncementScheduler` | `0 0 9 * * *` | 매일 09:00 생일 공지 |
| `WeeklyReportScheduler` | `0 5 0 * * MON` | 월요일 00:05 주간 리포트 |
| `OutboundEventCleanupScheduler` | `0 0 3 * * *` | 매일 03:00, 90일 지난 `outbound_events` 삭제 |

코드:

- `backend/src/main/java/com/vgc/service/AttendanceScheduler.java`
- `backend/src/main/java/com/vgc/service/EventScheduler.java`
- `backend/src/main/java/com/vgc/service/BirthdayAnnouncementScheduler.java`
- `backend/src/main/java/com/vgc/service/WeeklyReportScheduler.java`
- `backend/src/main/java/com/vgc/service/OutboundEventCleanupScheduler.java`

---

## §2. WebSocket / STOMP

| 항목 | 값 |
|---|---|
| 엔드포인트 | `/ws` |
| 브로커 | `/topic` (`enableSimpleBroker`) |
| 앱 prefix | `/app` |
| 채팅 핸들러 | `@MessageMapping("/chat/{conversationId}")` |

코드:

- `backend/src/main/java/com/vgc/config/WebSocketConfig.java`
- `backend/src/main/java/com/vgc/controller/ChatMessageController.java`
- `frontend/src/lib/websocket.ts`
- `frontend/src/components/ChatRoom.tsx`

프론트 기본 URL: `NEXT_PUBLIC_WS_URL` 또는 `ws://localhost:8080/ws`.

---

## §3. 배포 루프 (상세 규칙은 shared/70-policy)

1. 로컬 수정
2. **dev** compose 반영 · 수동 검증 (`https://dev.green-office.uk`)
3. 사용자에게 prod 확답
4. **prod** compose 반영

### Dev hot reload

`Dockerfile.dev` + 소스 bind-mount + `bootRun` / `next dev`.

| 변경 | 반영 |
|---|---|
| `backend/src/**` | Gradle continuous + devtools (약 2–5초) |
| `frontend/src/**`, `public/**`, Tailwind/postcss | `next dev` HMR |
| `backend/build.gradle` / `frontend/package.json` 의존성 | 해당 서비스 `--build` |
| `Dockerfile.dev` | 해당 서비스 rebuild |
| compose 볼륨 추가 | recreate |

코드만 고칠 때 `--build` 금지.

stale `.next` 캐시:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev stop frontend
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev rm -f frontend
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev up -d --no-deps frontend
```

Prod는 `bootJar` + `next build`. 소스 마운트 없음.

---

## §4. 모니터링

헬스 URL은 없다. 확인은 컨테이너 로그와 nginx 프록시.

```bash
docker logs greenforest-backend-dev
docker logs greenforest-backend-prod
docker compose -p green-forest-dev -f docker-compose.dev.yml ps
```

prod 파일 로그 볼륨 `green-forest-prod_logs_prod` → `/app/logs/` (`app.log`, `error.log`, `activity.log`).

stdout에 배너만 보이면:

```bash
docker run --rm -v green-forest-prod_logs_prod:/app/logs --entrypoint="" green-forest-prod-backend:latest sh -c "tail -100 /app/logs/error.log"
```

백업 크론: `scripts/backup-prod.sh` (서버 crontab `0 3 * * *` KST).
