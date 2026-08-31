---
title: docs — 문서 지도 & Doc-Sync 트리거 맵
last_updated: 2026-08-31
---

# docs/_index.md — 문서 지도 & Doc-Sync 트리거 맵

> **SSOT 해결 규칙**: 충돌 시 **코드(runtime) > 이 문서 > 다른 문서** 순으로 우선한다.
> 새 컨텍스트를 시작할 때 이 파일을 첫 번째로 읽는다.

## §1. 계층 인덱스 (대분류 × 계층)

| 계층 | `backend/` | `frontend/` | `env/` | `shared/` |
|---|---|---|---|---|
| **10** context | `10-context.md` | `10-context.md` | `10-context.md` | `10-context.md` |
| **20** containers | — | — | `20-containers.md` 🏛 | — |
| **30** components | `30-components/` (3) 🏛 | `30-components/` (1) | — | — |
| **40** data | `40-data.md` 🏛 | — | — | — |
| **50** api | — | — | — | — |
| **60** runtime | `60-runtime.md` | — | — | — |
| **70** policy | `70-policy.md` | `70-policy.md` | — | `70-policy.md` 🏛 |
| **90** adr | — | — | — | `90-adr/` (1) |

🏛 = 그 주제의 전역 권위본. `docs/env/`는 코드 폴더 `env/`가 아니라 compose·nginx 문서다.

경로:

- `docs/backend/10-context.md`
- `docs/backend/30-components/README.md`
- `docs/backend/30-components/controller-structure.md`
- `docs/backend/30-components/entity-and-enum.md`
- `docs/backend/30-components/service-repository.md`
- `docs/backend/40-data.md`
- `docs/backend/60-runtime.md`
- `docs/backend/70-policy.md`
- `docs/frontend/10-context.md`
- `docs/frontend/30-components/README.md`
- `docs/frontend/30-components/component-tree.md`
- `docs/frontend/70-policy.md`
- `docs/env/10-context.md`
- `docs/env/20-containers.md`
- `docs/shared/10-context.md`
- `docs/shared/70-policy.md`
- `docs/shared/90-adr/README.md`
- `docs/shared/90-adr/0001-original-spec-archived-from-worktree.md`

## §2. 작업별 진입 문서

| 작업 | 1차 진입(이것만 읽기) | 2차(필요 시) | 실제 코드 확인 |
|---|---|---|---|
| 시스템 전체 | `docs/shared/10-context.md` | `docs/env/20-containers.md` | `docker-compose.dev.yml` |
| 백엔드 API/컨트롤러 | `docs/backend/30-components/controller-structure.md` | `docs/backend/10-context.md` | `backend/src/main/java/com/vgc/controller/**/*.java` |
| 스키마·마이그레이션 | `docs/backend/40-data.md` | `docs/shared/70-policy.md` | `backend/src/main/java/com/vgc/entity/**/*.java` |
| 스케줄·채팅 | `docs/backend/60-runtime.md` | — | `backend/src/main/java/com/vgc/service/AttendanceScheduler.java` |
| 배포·사고 규칙 | `docs/shared/70-policy.md` | `docs/backend/70-policy.md` | `docker-compose.prod.yml` |
| 프론트 페이지 | `docs/frontend/30-components/component-tree.md` | `docs/frontend/70-policy.md` | `frontend/src/app/**/page.tsx` |
| 원본 기획 아카이브 | `docs/shared/90-adr/0001-original-spec-archived-from-worktree.md` | — | — |

## §3. 🚨 런타임 자산 (이동 금지)

compose가 `docs/`를 마운트하지 않는다 (2026-08-31 `grep` 실측). 이 저장소에 볼륨 마운트되는 문서 트리는 없다.

옮기면 안 되는 **인프라 파일**(문서 아님): `docker-compose.dev.yml`, `docker-compose.prod.yml`, `nginx/nginx.dev.conf`, `nginx/nginx.prod.conf`, `frontend/.env.production`.

## §4. 문서 권위 그래프

1. 실행 코드 (`backend/`, `frontend/`, compose, nginx)
2. 이 파일 (`docs/_index.md`)
3. `docs/shared/70-policy.md` (배포·사고)
4. 대분류 계층 문서
5. `AGENTS.md`
6. ADR (`docs/shared/90-adr/`) — 역사. 현재 동작을 이기지 않음

## §5. Doc-Sync 트리거 맵

| # | 코드 영역 (glob) | 갱신 대상 문서 | 등급 |
|---|---|---|---|
| 1 | `backend/src/main/java/com/vgc/controller/**/*.java` | `docs/backend/30-components/controller-structure.md` | M |
| 2 | `backend/src/main/java/com/vgc/entity/**/*.java` | `docs/backend/40-data.md`, `docs/backend/30-components/entity-and-enum.md` | M |
| 3 | `backend/src/main/resources/db/migration/*.sql` | `docs/backend/40-data.md` | M |
| 4 | `backend/db-migrations/*.sql` | `docs/backend/40-data.md` | M |
| 5 | `backend/src/main/resources/application-dev.properties` | `docs/backend/40-data.md` | C |
| 6 | `backend/src/main/resources/application-prod.properties` | `docs/backend/40-data.md` | M |
| 7 | `backend/src/main/java/com/vgc/service/*Scheduler.java` | `docs/backend/60-runtime.md` | M |
| 8 | `backend/src/main/java/com/vgc/config/WebSocketConfig.java` | `docs/backend/60-runtime.md` | M |
| 9 | `docker-compose.dev.yml` | `docs/env/20-containers.md` | M |
| 10 | `docker-compose.prod.yml` | `docs/env/20-containers.md` | M |
| 11 | `nginx/nginx.dev.conf` | `docs/env/20-containers.md` | C |
| 12 | `nginx/nginx.prod.conf` | `docs/env/20-containers.md` | C |
| 13 | `frontend/src/app/**/page.tsx` | `docs/frontend/30-components/component-tree.md` | M |
| 14 | `frontend/src/context/*.tsx` | `docs/frontend/10-context.md` | M |
| 15 | `frontend/src/lib/websocket.ts` | `docs/backend/60-runtime.md`, `docs/frontend/10-context.md` | C |
| 16 | `backend/src/main/java/com/vgc/config/SecurityConfig.java` | `docs/backend/10-context.md` | C |
| 17 | `backend/src/main/java/com/vgc/security/NotifyTokenFilter.java` | `docs/backend/30-components/controller-structure.md` | C |

포트·프로필·ddl-auto 변경 시: `docs/env/20-containers.md` + `docs/backend/40-data.md` + `docs/shared/70-policy.md`.

## §6. Code → Docs 역인덱스

| 코드 경로 접두 | 소유 모듈 | 먼저 읽을 문서 | 권위본 |
|---|---|---|---|
| `backend/src/main/java/com/vgc/controller/` | backend | `docs/backend/30-components/controller-structure.md` | 동 문서 |
| `backend/src/main/java/com/vgc/entity/` | backend | `docs/backend/40-data.md` | 동 문서 |
| `backend/src/main/java/com/vgc/service/` | backend | `docs/backend/30-components/service-repository.md` | `docs/backend/60-runtime.md` (스케줄) |
| `backend/src/main/resources/db/` | backend | `docs/backend/40-data.md` | 동 문서 |
| `backend/db-migrations/` | backend | `docs/backend/40-data.md` | 동 문서 |
| `frontend/src/app/` | frontend | `docs/frontend/30-components/component-tree.md` | 동 문서 |
| `frontend/src/components/` | frontend | `docs/frontend/30-components/component-tree.md` | 동 문서 |
| `docker-compose.dev.yml` | env | `docs/env/20-containers.md` | 동 문서 |
| `nginx/` | env | `docs/env/20-containers.md` | 동 문서 |
