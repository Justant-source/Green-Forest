---
title: L1 Context — backend 경계 · 액터 · 외부 의존
last_updated: 2026-08-31
---

# backend/10-context — 비즈니스 도메인 · 외부 의존성 · 액터

> Spring Boot 애플리케이션(`com.vgc`)의 경계. 내부 패키지 구조는 `30-components/`에 있다.

---

## §1. 이 모듈이 하는 일

사내 커뮤니티 플랫폼 **그린 포레스트**의 서버. 게시판·댓글·설문·출석·가챠·퀘스트·정원(식물)·랭킹·실시간 채팅·사진전·포토빙고를 HTTP/WebSocket으로 제공한다.

스택 (코드 기준): Spring Boot 3.2.5, Java 17 (`backend/build.gradle`), Spring Security 6 + JWT, MySQL 8.0, Spring Data JPA.

**LLM 연동은 없다.**

---

## §2. 액터

| 액터 | 진입 | 비고 |
|---|---|---|
| 임직원 사용자 | `/api/**` + JWT | 회원가입·피드·채팅 |
| 관리자 | `/api/admin/**` | 카테고리·이벤트·가챠·드랍 |
| Notify 폴러 | `/api/notify/**` | `NotifyTokenFilter` Bearer 토큰 |
| STOMP 클라이언트 | `/ws` | JWT를 CONNECT 헤더로 전달 |

---

## §3. 외부 의존

| 의존 | 역할 | 코드 |
|---|---|---|
| MySQL 8.0 | 상태 저장 | 컨테이너 hostname `mysql`. 호스트 포트는 compose가 정함 |
| (선택) S3 | 이미지 | `backend/src/main/java/com/vgc/service/S3Service.java` — 로컬 저장과 병행 |
| 사내 Notify 폴러 | `outbound_events` 아웃박스 소비 | `backend/src/main/java/com/vgc/security/NotifyTokenFilter.java` |

OAuth·결제·외부 LLM은 사용하지 않는다.

---

## §4. 인증

JWT (`backend/src/main/java/com/vgc/security/JwtUtil.java`). 필터 체인: `backend/src/main/java/com/vgc/config/SecurityConfig.java`.

---

## §5. 이 문서가 담지 않는 것

개발자 머신 셋업, Gradle/npm 설치, 문서 린터 설정.
