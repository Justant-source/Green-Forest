---
title: L7 Policy — 전역 규칙 · 사고 이력
last_updated: 2026-08-31
---

# shared/70-policy — 배포 · 환경 구분 · 보안 사고

> **이 파일이 배포 순서의 전역 권위본이다.** backend/70-policy · frontend/70-policy는 세부만 적는다.
> 산문 규칙에 대응하는 **코드 게이트는 0개.** pre-commit은 문서 린트만 돌린다.

---

## §1. 배포 순서 (README 규칙 0–5)

| 규칙 | 내용 |
|---|---|
| 0 | 기본 스코프는 **dev**. "바꿔줘"만 있으면 prod를 건드리지 않는다. prod는 "prod / 운영 / 실서버" 명시 시에만. |
| 1 | 사용자 트래픽에 영향 있는 배포는 **dev → 검증 → prod**. |
| 2 | dev와 prod를 **동시에** `up --build` 하지 않는다. |
| 3 | prod 직전 "dev 테스트 끝났습니까?" 확답. |
| 4 | dev 실패·미완료면 동일 변경을 prod에 넣지 않는다. |
| 5 | 예외(긴급 롤백, prod DB 긴급)는 **명시 지시**만. |

```bash
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d --build <svc>
# 수동 검증 https://dev.green-office.uk
# 사용자 확답 후
docker compose -p green-forest-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build <svc>
```

**코드 게이트 없음.**

---

## §2. Docker 조작 (사고 2026-04-21)

- `xargs docker stop` / 광범위 `docker ps | xargs` **금지.** 컨테이너 이름을 명시한다. 예: `docker stop greenforest-backend-dev`.
- 프로젝트명 분리: `-p green-forest-dev` / `-p green-forest-prod`. 공통 `latest` 태그 공유 금지.
- `stop/rm/restart` 전 `docker ps --filter name=...` 로 대상만 나열하고 사용자 확인. prod가 섞이면 중단.
- prod 에러는 파일 로그. `docker logs`에 배너만 있으면 `green-forest-prod_logs_prod` 볼륨을 본다.

**코드 게이트 없음.**

---

## §3. Prod DB 마이그레이션 (사고 2026-04-27)

- `ENUM(...)` / `DATETIME(6)` — `docs/backend/40-data.md` §5.
- Flyway 런타임 없음. 수작업 SQL.
- **코드 게이트 없음.** Hibernate validate 실패 = 재시작 루프.

---

## §4. 실 사용자 계정 (사고 2026-04-22)

- prod 실사용자 비밀번호를 테스트로 바꾸지 않는다. BCrypt는 원복 불가.
- 인증 테스트는 `gm` / `admin`만.
- 바꿨다면 변경 전 해시를 메모하고 즉시 원복. 못 하면 즉시 보고.

**코드 게이트 없음.**

---

## §5. 환경 이름

`dev` / `prod`만. `local` 금지. `application-prd.properties`는 미사용 잔재.

볼륨 `name:` 변경 금지 (`docs/env/20-containers.md` §3).

---

## §6. Frontend URL (사고 2026-04-21)

`NEXT_PUBLIC_API_BASE_URL`은 Cloudflare 도메인만. IP·내부 포트 금지. 세부: `docs/frontend/70-policy.md`.

---

## §7. 결론

배포 순서·Docker 안전·DB 타입·실계정·URL 규칙은 **전부 수동.** 문서 린트 hook은 이 규칙을 집행하지 않는다.
