---
title: L3 — Service · Repository · 트랜잭션
last_updated: 2026-08-31
---

# backend/30-components/service-repository

---

## §1. 계층 책임

| 계층 | 위치 | 책임 |
|---|---|---|
| Controller | `backend/src/main/java/com/vgc/controller/` | HTTP/STOMP 입출력 |
| Service | `backend/src/main/java/com/vgc/service/` | 비즈니스 규칙, `@Transactional` 경계 |
| Repository | `backend/src/main/java/com/vgc/repository/` | Spring Data JPA 쿼리 메서드 · `@EntityGraph` |
| Entity | `backend/src/main/java/com/vgc/entity/` | 테이블 매핑 |

서비스 파일 40개 (2026-08-31). 스케줄러(`*Scheduler.java`)도 같은 패키지에 둔다.

---

## §2. 트랜잭션

기본은 서비스 메서드의 `@Transactional`. 조회는 `@Transactional(readOnly = true)`를 쓰는 곳이 있다 (예: `GachaService`, `AttendanceService`).

출석 추첨은 `AttendanceDrawCoordinator`의 `ReentrantLock`으로 스케줄러와 관리자 API를 직렬화한다.

코드: `backend/src/main/java/com/vgc/service/AttendanceDrawCoordinator.java`

---

## §3. 스케줄러와 WebSocket 풀 분리

`@EnableScheduling`은 `backend/src/main/java/com/vgc/VgcApplication.java`.

`backend/src/main/java/com/vgc/config/SchedulingConfig.java`가 `taskScheduler` 빈(pool=1)을 등록해 STOMP 브로커 스케줄러와 `@Scheduled`를 분리한다.
