# 그린오피스 서버 작업지시서 (집 / www.green-office.uk)

## 0. 개요

사내망에서 5분/30초 주기로 polling 해갈 수 있도록 **Outbox + 조회 API** 를 그린오피스 서버에 추가한다. 동시에 외부에서 API 명세를 시각적으로 볼 수 있도록 **Swagger UI** 를 공개한다.

발생시켜야 할 이벤트는 두 가지다:
- `ATTENDANCE_WINNER` — 매일 11:00 KST 출석 추첨 당첨자 1명 선정 시
- `GACHA_WIN` — 사용자가 가챠에서 당첨됐을 때 (`isWinner == true`)

두 지점 모두 이미 `notificationService.createNotification(...)` 가 호출되는 자리가 있다. 그 옆에 outbox publish 한 줄만 추가한다.

---

## 1. 작업 범위 (체크리스트)

- [ ] **1.1** `outbound_events` 테이블 생성 (MySQL)
- [ ] **1.2** `OutboundEvent` JPA Entity + Repository 추가
- [ ] **1.3** `OutboundEventService.publish(...)` 구현
- [ ] **1.4** `AttendanceService.drawDailyWinner()` 에 publish 호출 추가
- [ ] **1.5** `GachaService.draw()` 의 `isWinner == true` 블록에 publish 호출 추가
- [ ] **1.6** `NotifyEventController` (조회 API) 추가 — `GET /api/notify/events`
- [ ] **1.7** API Token 인증 필터 추가 (Bearer token 한 개만 비교)
- [ ] **1.8** `SecurityConfig` 수정 — `/api/notify/**` 는 토큰 인증 적용
- [ ] **1.9** Swagger (springdoc-openapi) 의존성 추가 + 설정
- [ ] **1.10** `SecurityConfig` 에 swagger 경로 `permitAll`
- [ ] **1.11** `OutboundEventCleanupScheduler` — 90일 지난 이벤트 삭제
- [ ] **1.12** application properties 에 `app.notify.token` 추가
- [ ] **1.13** prod 환경변수에 `NOTIFY_API_TOKEN` 주입
- [ ] **1.14** 통합 테스트 — curl 로 publish → fetch 동작 확인

---

## 2. 기술 결정 사항 (확정)

| 항목 | 결정 |
|---|---|
| DB | 기존 MySQL 사용 (PostgreSQL 아님 주의) |
| 마이그레이션 | 수동 SQL 파일 + JPA Entity (Flyway 미도입 상태 유지) |
| 인증 | Bearer Token (단일 정적 토큰, 환경변수 주입) |
| 토큰 비교 | `MessageDigest.isEqual(...)` 으로 timing-safe 비교 |
| Swagger | springdoc-openapi 사용, 공개 (`/swagger-ui.html`, `/v3/api-docs`) |
| Swagger 노출 정책 | 전체 공개. **단 `/api/notify/**` 는 `@Hidden` 처리해 문서에서 제외** |
| 이벤트 보존 | 90일, 매일 03:00 KST cleanup |

---

## 3. 상세 작업

### 3.1 DB 테이블 생성

**파일**: `backend/db-migrations/add_outbound_events.sql` (기존 디렉토리 컨벤션 따름)

```sql
CREATE TABLE outbound_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL,
    payload     JSON         NOT NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_outbound_events_id_type (id, event_type),
    INDEX idx_outbound_events_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> JPA `ddl-auto=update` 로 Entity 추가 시 자동 생성되긴 하지만, prod 는 `validate` 라서 SQL 을 먼저 실행해야 한다. **dev 든 prod 든 위 SQL 을 먼저 실행하고 배포할 것.**

### 3.2 Entity / Repository

**파일**: `backend/src/main/java/com/vgc/entity/OutboundEvent.java`

```java
package com.vgc.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_events")
public class OutboundEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload; // JSON string

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // getters / setters 생략 — 모두 추가
}
```

**파일**: `backend/src/main/java/com/vgc/repository/OutboundEventRepository.java`

```java
package com.vgc.repository;

import com.vgc.entity.OutboundEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboundEventRepository extends JpaRepository<OutboundEvent, Long> {

    @Query("SELECT e FROM OutboundEvent e WHERE e.id > :sinceId ORDER BY e.id ASC")
    List<OutboundEvent> findSince(Long sinceId, Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
```

### 3.3 OutboundEventService

**파일**: `backend/src/main/java/com/vgc/service/OutboundEventService.java`

```java
package com.vgc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgc.entity.OutboundEvent;
import com.vgc.repository.OutboundEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OutboundEventService {

    private static final Logger log = LoggerFactory.getLogger(OutboundEventService.class);

    private final OutboundEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboundEventService(OutboundEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 호출하는 쪽의 트랜잭션에 join 한다 (winner 저장과 같은 커밋에 묶이도록).
     * publish 자체 실패가 비즈니스 로직을 깨뜨리지 않도록 try-catch 로 감싼다.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(String eventType, Map<String, Object> payload) {
        try {
            OutboundEvent e = new OutboundEvent();
            e.setEventType(eventType);
            e.setPayload(objectMapper.writeValueAsString(payload));
            repository.save(e);
        } catch (JsonProcessingException ex) {
            log.error("OutboundEvent payload JSON 변환 실패: type={}, payload={}", eventType, payload, ex);
        } catch (Exception ex) {
            log.error("OutboundEvent publish 실패: type={}", eventType, ex);
        }
    }
}
```

### 3.4 AttendanceService 수정

**파일**: `backend/src/main/java/com/vgc/service/AttendanceService.java`

생성자에 `OutboundEventService` 추가하고, `drawDailyWinner(LocalDate date)` 메서드의 `notificationService.createNotification(...)` **바로 다음 줄에** 추가:

```java
// 외부 알림용 outbox publish (사내 우분투가 polling 해감)
outboundEventService.publish("ATTENDANCE_WINNER", Map.of(
    "date", date.toString(),
    "winnerNickname", winner.getUser().getNickname(),
    "winnerId", winner.getUser().getId(),
    "totalCheckins", checkins.size(),
    "drawnAt", LocalDateTime.now(KST).toString()
));
```

> 같은 `@Transactional` 안이므로 winner 저장 + outbox 저장이 원자적 커밋된다.

### 3.5 GachaService 수정

**파일**: `backend/src/main/java/com/vgc/service/GachaService.java`

생성자에 `OutboundEventService` 추가. `draw(...)` 메서드 안의 `if (isWinner) { ... notificationService ... }` 블록 안, `createNotification(...)` 다음 줄에 추가:

```java
outboundEventService.publish("GACHA_WIN", Map.of(
    "drawId", saved.getId(),
    "userId", user.getId(),
    "userNickname", user.getNickname(),
    "prizeName", prize.getName(),
    "prizeCashValue", prize.getCashValue()
));
```

### 3.6 NotifyEventController

**파일**: `backend/src/main/java/com/vgc/controller/NotifyEventController.java`

```java
package com.vgc.controller;

import com.vgc.entity.OutboundEvent;
import com.vgc.repository.OutboundEventRepository;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Hidden  // Swagger 문서에서 제외 — 외부 노출 불필요
@RestController
@RequestMapping("/api/notify")
public class NotifyEventController {

    private final OutboundEventRepository repository;

    public NotifyEventController(OutboundEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events(
            @RequestParam(name = "since", defaultValue = "0") long sinceId,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {

        if (limit < 1) limit = 1;
        if (limit > 500) limit = 500;

        List<OutboundEvent> rows = repository.findSince(sinceId, PageRequest.of(0, limit));
        return rows.stream().map(e -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("eventType", e.getEventType());
            m.put("payload", e.getPayload()); // JSON 문자열 그대로 — 사내에서 파싱
            m.put("createdAt", e.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());
    }
}
```

### 3.7 토큰 인증 필터

**파일**: `backend/src/main/java/com/vgc/security/NotifyTokenFilter.java`

```java
package com.vgc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class NotifyTokenFilter extends OncePerRequestFilter {

    private final byte[] expectedTokenBytes;

    public NotifyTokenFilter(@Value("${app.notify.token:}") String token) {
        this.expectedTokenBytes = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/notify/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (expectedTokenBytes.length == 0) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "notify token not configured");
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing bearer token");
            return;
        }
        byte[] presented = header.substring(7).getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(presented, expectedTokenBytes)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid token");
            return;
        }

        // 인증 성공으로 표시 (anonymous 가 아닌 임의의 authenticated 토큰)
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("notify-poller", "notify-poller",
                        AuthorityUtils.createAuthorityList("ROLE_NOTIFY_POLLER"))
        );
        chain.doFilter(req, res);
    }
}
```

### 3.8 SecurityConfig 수정

`requestMatchers` 체인에 다음 두 줄을 **다른 규칙들보다 위쪽에** 추가:

```java
// Notify polling — 별도 토큰 필터에서 인증
.requestMatchers("/api/notify/**").authenticated()

// Swagger UI — 공개
.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

그리고 `NotifyTokenFilter` 를 JWT 필터 이전에 등록:

```java
http.addFilterBefore(notifyTokenFilter, UsernamePasswordAuthenticationFilter.class);
```

### 3.9 Swagger 의존성

**파일**: `backend/build.gradle`

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
```

**파일**: `backend/src/main/java/com/vgc/config/OpenApiConfig.java`

```java
package com.vgc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI greenOfficeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Green Office API")
                .version("1.0")
                .description("Public API documentation for green-office.uk"));
    }
}
```

> Swagger UI: `https://www.green-office.uk/swagger-ui.html`
> OpenAPI JSON: `https://www.green-office.uk/v3/api-docs`

> nginx 의 `/api/` 외에 `/swagger-ui/`, `/v3/api-docs` 도 backend 로 프록시 되어야 한다 — `nginx.prod.conf` 에 location 추가:

```nginx
location ~ ^/(swagger-ui|v3/api-docs) {
    proxy_pass http://backend:9090;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 3.10 Cleanup Scheduler

**파일**: `backend/src/main/java/com/vgc/service/OutboundEventCleanupScheduler.java`

```java
package com.vgc.service;

import com.vgc.repository.OutboundEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class OutboundEventCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboundEventCleanupScheduler.class);
    private final OutboundEventRepository repository;

    public OutboundEventCleanupScheduler(OutboundEventRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(90);
        long n = repository.deleteByCreatedAtBefore(cutoff);
        log.info("OutboundEvent cleanup: {} rows deleted (older than {})", n, cutoff);
    }
}
```

### 3.11 application properties

**`application-prod.properties`** 에 추가:
```properties
# Notify polling token (사내 우분투 폴러만 알아야 함)
app.notify.token=${NOTIFY_API_TOKEN}
```

**`application-dev.properties`** 에 추가:
```properties
app.notify.token=${NOTIFY_API_TOKEN:dev-local-token-please-change}
```

### 3.12 환경변수 (prod)

`.env.prod` 또는 docker-compose 환경변수에 추가:
```
NOTIFY_API_TOKEN=<openssl rand -hex 32 결과>
```

생성 명령:
```bash
openssl rand -hex 32
```

이 값을 사내 우분투 담당자에게 **별도 채널** (메신저 DM, 1Password 등) 로 전달.

---

## 4. 검증 시나리오

배포 후 그린오피스 서버에서 직접 검증:

```bash
# 1. Swagger 접근 (토큰 불필요)
curl -s https://www.green-office.uk/v3/api-docs | head -20
# 브라우저: https://www.green-office.uk/swagger-ui.html

# 2. Notify 엔드포인트 — 토큰 없이는 401
curl -i https://www.green-office.uk/api/notify/events
# → HTTP/1.1 401 Unauthorized

# 3. 잘못된 토큰 → 401
curl -i -H "Authorization: Bearer wrong" https://www.green-office.uk/api/notify/events
# → HTTP/1.1 401 Unauthorized

# 4. 올바른 토큰 → 200 + JSON 배열
TOKEN=<NOTIFY_API_TOKEN 값>
curl -s -H "Authorization: Bearer $TOKEN" \
     "https://www.green-office.uk/api/notify/events?since=0&limit=10" | jq .
# → [] (아직 이벤트 없으면 빈 배열)

# 5. DB 에 가짜 이벤트 한 건 넣고 다시 조회
mysql -u root -p vgc_db -e "
  INSERT INTO outbound_events(event_type, payload)
  VALUES('GACHA_WIN', JSON_OBJECT('test', true, 'prizeName', 'TEST'));"
curl -s -H "Authorization: Bearer $TOKEN" \
     "https://www.green-office.uk/api/notify/events?since=0&limit=10" | jq .
# → 한 건 반환됨
```

---

## 5. 배포 순서

1. SQL 먼저 실행 (`add_outbound_events.sql`)
2. 환경변수 `NOTIFY_API_TOKEN` 주입
3. 백엔드 빌드 & 배포 (Docker)
4. nginx 설정 reload
5. 위 §4 시나리오 1~5 모두 통과 확인
6. 사내 담당자에게 토큰 + 엔드포인트 URL 전달

---

## 6. 주의사항

- `OutboundEventService.publish` 는 **호출자의 트랜잭션에 반드시 join** 되어야 한다. `Propagation.REQUIRES_NEW` 로 잘못 바꾸면 winner 저장 롤백 시에도 이벤트가 발송되어 사내에 가짜 알림이 간다.
- `NotifyTokenFilter` 는 `MessageDigest.isEqual` 로 비교한다. `equals()` 나 `==` 로 바꾸지 말 것 (timing attack).
- Swagger 노출 후, `/api/admin/**` 같은 관리자 엔드포인트도 스펙에 노출된다는 점은 인지하고 진행. URL 노출만으로 호출되는 건 아니지만, 공격 표면이 한눈에 보인다는 점은 사실. 우려된다면 추후 `@Hidden` 으로 admin 컨트롤러도 가릴 수 있음.
- prod 의 `ddl-auto=validate` 때문에 Entity 와 실제 테이블이 안 맞으면 부팅 실패한다. SQL 먼저 실행.
