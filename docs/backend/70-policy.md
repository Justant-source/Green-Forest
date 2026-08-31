---
title: L7 Policy — backend 운영 제약 (게이트 없음)
last_updated: 2026-08-31
---

# backend/70-policy — 배포·DB·환경 파일

> 전역 정본은 `docs/shared/70-policy.md`. 이 문서는 백엔드에 붙는 세부를 적는다.
> **코드/hook이 배포 순서를 강제하지 않는다.**

---

## §1. 배포 순서

shared/70-policy §1과 동일. Docker 이미지·컨테이너·`.env.prod`·nginx는 항상 dev 검증 후 prod.

---

## §2. Prod DB

- `spring.jpa.hibernate.ddl-auto=validate` — 스키마를 자동으로 바꾸지 않음.
- 새 컬럼/테이블은 수작업 SQL. 타입은 dev `SHOW CREATE TABLE` 복사.
- `@Enumerated(STRING)` → `ENUM(...)`, `LocalDateTime` → `DATETIME(6)`.
- Flyway 스타터 없음. `V*.sql` 파일만 두고 자동 적용하지 않음. 상세: [40-data.md](40-data.md).

**코드 게이트 없음.** 잘못된 SQL은 prod 재시작 루프로만 드러난다 (사고 2026-04-27).

---

## §3. 환경 파일 명명

허용 이름: `dev` / `prod` 만. `local` 금지.

| 종류 | 파일 |
|---|---|
| Compose | `docker-compose.dev.yml`, `docker-compose.prod.yml` |
| Env | `.env.dev`, `.env.prod` |
| Spring | `application-dev.properties`, `application-prod.properties` |
| Profile | `SPRING_PROFILES_ACTIVE=dev` 또는 `prod` |

`application-prd.properties`는 레포에 남아 있으나 **미사용**. 프로필 이름 `prd`를 새로 쓰지 마라.

---

## §4. 이 계층의 한계

pre-commit은 `python3 scripts/lint_docs.py`만 강제한다. 배포 순서·DB 타입·실사용자 비밀번호는 자동화하지 않는다.
