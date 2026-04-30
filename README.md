# 그린 포레스트 (Green Forest)

사내 커뮤니티 플랫폼. Spring Boot + Next.js + MySQL 기반.

---

## 🚨 배포 순서 규칙 (Dev → Prod)

**반드시 `dev` 환경에서 먼저 적용하고 테스트를 완료한 뒤에만 `prod`에 반영한다.** 순서 위반은 절대 금지.

| 규칙 | 내용 |
|------|------|
| 0 | **기본 변경 스코프는 dev.** "바꿔줘 / 수정해줘 / 변경해줘 / 적용해줘" 같은 지시는 **dev에만 적용**한다. prod는 사용자가 "prod / 운영 / 실서버" 등을 **명시적으로 언급**할 때만 대상이 된다. |
| 1 | Docker 이미지 빌드 / 컨테이너 recreate / `.env` 반영 / nginx·cloudflared 설정 변경 등 **사용자 트래픽에 영향을 주는 모든 배포는 dev → prod 순서로 진행**한다. |
| 2 | **dev와 prod를 동시에 빌드·재배포하는 것은 금지.** 한 번에 양쪽 `docker compose up --build` 절대 불가. |
| 3 | prod 적용 **직전에 "dev에서 테스트 완료됐습니까?"를 반드시 확인**한다. 확답 없으면 prod 변경 보류. |
| 4 | dev 테스트가 실패했거나 미완료면 **동일 변경을 prod에 적용하지 않는다.** |
| 5 | 예외 (prod 긴급 롤백, prod DB 긴급 대응 등)는 **사용자의 명시적 지시**가 있을 때만 허용. |

### 권장 플로우

```
┌─────────────────────────────┐
│ 1. dev에 변경 적용           │
│    docker compose           │
│    -p green-forest-dev      │
│    -f docker-compose.dev.yml│
│    up -d --build <svc>      │
└──────────┬──────────────────┘
           ↓
┌─────────────────────────────┐
│ 2. dev 기능 검증             │
│    (https://dev.green-      │
│     office.uk에서 수동 확인) │
└──────────┬──────────────────┘
           ↓
   OK? ──▶ NO ──▶ 원인 분석 / 수정 (1로 복귀)
    │
   YES
    ↓
┌─────────────────────────────┐
│ 3. "prod 적용해도 되나요?"   │
│    사용자 확답 획득           │
└──────────┬──────────────────┘
           ↓
┌──────────────────────────────┐
│ 4. prod에 동일 변경 적용      │
│    docker compose            │
│    -p green-forest-prod      │
│    -f docker-compose.prod.yml│
│    up -d --build <svc>       │
└──────────────────────────────┘
```

---

## 환경 구성

### 전제 조건
- Docker Desktop 설치
- 프로젝트 루트에 환경변수 파일 준비

```bash
cp .env.dev.example  .env.dev    # 값 채우기
cp .env.prod.example .env.prod   # 값 채우기 (서버에서)
```

> ⚠️ **`NEXT_PUBLIC_*` 값 변경 시 프론트엔드 재빌드 필수 (Prod만).** prod 는 `next build`로 번들이 고정되므로 컨테이너 restart만으로는 반영 안 됨.
> ```bash
> docker compose -p green-forest-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build frontend
> ```
> **Dev 는 `next dev` + `env_file` 런타임 주입 구조라 재빌드 불필요, `restart` 만 하면 반영된다.**
> ```bash
> docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev restart frontend
> ```
> 백엔드 CORS(`CORS_ORIGINS`)도 런타임 환경변수라 `--force-recreate`만 하면 반영된다.

> ⚠️ **`NEXT_PUBLIC_API_BASE_URL`은 반드시 Cloudflare 도메인을 써야 한다.** 서버 IP나 내부 포트를 직접 쓰면, IP가 바뀌거나 방화벽에 막혔을 때 브라우저에서 글·이미지·랭킹 등 API 데이터 전체가 로딩 안 된다.
> - `frontend/.env.dev` → `NEXT_PUBLIC_API_BASE_URL=https://dev.green-office.uk/api`
> - `frontend/.env.prod` (또는 루트 `.env.prod`) → `NEXT_PUBLIC_API_BASE_URL=https://green-office.uk/api`

---

## Dev 환경

### 실행 (최초 1회만 `--build`)

```bash
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up --build -d
```

첫 기동은 Gradle 의존성 다운로드 + `npm install` + Next.js 첫 페이지 컴파일 때문에 2~4분 걸린다. 이후엔 컨테이너를 내리지 않는 한 **코드 변경은 저장만으로 반영** 된다 (아래 "Hot Reload" 참고).

### ⚡ Hot Reload — 코드 저장 → 즉시 반영

dev 환경은 소스를 bind-mount 하고 컨테이너 안에서 `bootRun`(+`compileJava --continuous`) / `next dev` 가 돌아간다. **재빌드·재시작 없이 파일 저장만으로 반영된다.**

| 변경 유형 | 반영 시간 | 동작 |
|-----------|-----------|------|
| `backend/src/**/*.java` | 2~5초 | Gradle 재컴파일 → `spring-boot-devtools` 가 Spring context 재시작 |
| `frontend/src/**/*.{ts,tsx,css}` | 1~2초 | Next.js HMR, 브라우저 자동 갱신 |
| `frontend/public/**` | 1초 | HMR |
| `frontend/tailwind.config.ts`, `postcss.config.js` | ~3초 | `next dev` 자동 재컴파일 |

**저장만으로 해결 안 되고 재빌드가 필요한 경우:**
```bash
# 1) build.gradle 또는 package.json 의 의존성 추가/변경
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d --build backend   # (또는 frontend)

# 2) Dockerfile.dev 자체 수정
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d --build --no-deps <svc>

# 3) docker-compose.dev.yml 의 volumes 추가/변경
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d --force-recreate <svc>
```

**UI가 깨져 보이거나 CSS/이미지가 이상할 때 (stale `.next` 캐시):**
```bash
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev stop frontend
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev rm -f frontend
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d --no-deps frontend
```
브라우저에서 **Ctrl+Shift+R** 로 하드 리프레시.

**Hot reload 가 작동 안 할 때 로그 확인:**
```bash
docker logs -f greenforest-backend-dev  | grep -E "Change detected|BUILD SUCCESSFUL|Waiting for changes"
docker logs -f greenforest-frontend-dev | grep -E "Compiled|Ready in"
```
`Waiting for changes to input files...` 가 나오면 감시 중. `Change detected` → `BUILD SUCCESSFUL` 시퀀스가 안 나오면 bind-mount가 풀렸거나 파일 경로가 `src/` 밖에 있음.

> Prod 는 이 구조를 쓰지 않는다. Prod 는 그대로 `bootJar` + `next build` 로 고정 번들을 실행. Dev 전용 `Dockerfile.dev` / 소스 마운트는 Prod에 영향 없다.

### 접속 주소

| 서비스 | 주소 |
|--------|------|
| 웹 (Cloudflare) | https://dev.green-office.uk |
| 웹 (내부 Nginx) | http://localhost:8080 |
| Backend API | https://dev.green-office.uk/api |
| MySQL (외부) | localhost:**3308** |

### DB 정보

| 항목 | 값 |
|------|----|
| Host | localhost |
| Port | **3308** |
| Database | `vgc_db_dev` |
| Username | root |
| Password | `.env.dev`의 `DB_PASSWORD` |

### MySQL 클라이언트 접속 예시

```bash
mysql -h 127.0.0.1 -P 3308 -u root -p vgc_db_dev
```

---

## Prod (운영) 환경

### 실행

```bash
docker compose -p green-forest-prod -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

### 접속 주소

| 서비스 | 주소 |
|--------|------|
| 웹 (공개) | https://green-office.uk, https://www.green-office.uk |
| Dev 공개 | https://dev.green-office.uk (Dev 환경에 연결) |
| 웹 (Nginx, 내부) | http://서버IP:**80** |
| Backend API | https://green-office.uk/api |
| Backend | 외부 미노출 (nginx 경유) |
| MySQL | 외부 미노출 (컨테이너 내부) |

> 공개 접근은 **Cloudflare Tunnel**을 경유한다. HTTPS/인증서는 Cloudflare가 자동 처리. 자세한 내용은 아래 "공개 도메인 (Cloudflare Tunnel)" 섹션 참고.

### DB 정보

| 항목 | 값 |
|------|----|
| Host | mysql (컨테이너 내부 이름) |
| Port | **3306** (내부 전용) |
| Database | `vgc_db` |
| Username | root |
| Password | `.env.prod`의 `DB_PASSWORD` |

### Prod MySQL 접속 (서버 내부)

```bash
docker exec -it greenforest-mysql-prod mysql -uroot -p vgc_db
```

---

## 환경별 비교

| 항목 | Dev | Prod |
|------|-----|------|
| Spring Profile | `dev` | `prod` |
| DB 이름 | `vgc_db_dev` | `vgc_db` |
| MySQL 외부 포트 | **3308** | 미노출 |
| Backend 외부 포트 | 미노출 (nginx 경유) | 미노출 |
| Nginx 포트 | **8080** | **80** |
| DDL 정책 | `update` | `validate` |
| SQL 로그 | ON | OFF |
| Compose project name | `green-forest-dev` | `green-forest-prod` |
| 컨테이너 이름 prefix | `greenforest-*-dev` | `greenforest-*-prod` |
| 환경 파일 | `.env.dev`, `docker-compose.dev.yml`, `application-dev.properties` | `.env.prod`, `docker-compose.prod.yml`, `application-prod.properties` |

---

## DB 마이그레이션 (Prod → Dev, 최초 1회)

```bash
# 1. prod 서버에서 덤프
docker exec greenforest-mysql-prod \
  mysqldump -uroot -p${DB_PASSWORD} vgc_db > dump.sql

# 2. dump.sql을 dev 머신으로 복사 후 복원
docker exec -i greenforest-mysql-dev \
  mysql -uroot -p${DB_PASSWORD} vgc_db_dev < dump.sql
```

## 스키마 변경 (Dev → Prod, 배포 시)

prod는 DDL `validate` 모드이므로 자동으로 스키마를 바꾸지 않는다. dev에서 새 엔티티/컬럼을 추가했다면 prod 배포 전에 **비파괴적으로** 수동 반영해야 한다.

```bash
# 1. dev/prod 스키마 덤프
docker exec greenforest-mysql-dev  mysqldump -uroot -p${DB_PASSWORD} --no-data vgc_db_dev > dev_schema.sql
docker exec greenforest-mysql-prod mysqldump -uroot -p${DB_PASSWORD} --no-data vgc_db     > prod_schema.sql

# 2. diff로 누락분 확인 (CREATE TABLE / ALTER TABLE ADD COLUMN 만 허용, DROP 금지)
diff prod_schema.sql dev_schema.sql

# 3. prod DB 백업 후 누락분만 수동 적용
docker exec greenforest-mysql-prod mysqldump -uroot -p${DB_PASSWORD} vgc_db > prod_backup_$(date +%Y%m%d_%H%M%S).sql
docker exec -i greenforest-mysql-prod mysql -uroot -p${DB_PASSWORD} vgc_db < diff.sql
```

---

## 공개 도메인 (Cloudflare Tunnel)

외부 공개 접근은 Cloudflare Tunnel(`cloudflared`) 경유. 서버 방화벽 개방 / 포트포워딩 / 공인 IP 불필요. 아웃바운드만으로 Cloudflare 엣지와 암호화 터널 유지.

### 라우팅 구조

```
사용자 브라우저
    ↓ HTTPS (Cloudflare가 인증서 자동 관리)
Cloudflare 엣지
    ↓ 아웃바운드 터널 (QUIC)
cloudflared (서버, systemd 서비스)
    ↓ localhost
nginx 컨테이너 (prod: :80 / dev: :8080)
    ↓
backend / frontend
```

### 도메인 매핑

| 도메인 | 환경 | nginx 컨테이너 | ingress target |
|--------|------|-----------------|----------------|
| `green-office.uk` | Prod | `greenforest-nginx-prod` | `http://localhost:80` |
| `www.green-office.uk` | Prod | `greenforest-nginx-prod` | `http://localhost:80` |
| `dev.green-office.uk` | Dev | `greenforest-nginx-dev` | `http://localhost:8080` |

### nginx 설정 분리

prod / dev 컨테이너가 각자의 `server_name`을 갖도록 파일을 분리.

| 파일 | 용도 | 마운트 대상 |
|------|------|--------------|
| `nginx/nginx.prod.conf` | `server_name green-office.uk www.green-office.uk` | `docker-compose.prod.yml` |
| `nginx/nginx.dev.conf`  | `server_name dev.green-office.uk` | `docker-compose.dev.yml` |

### cloudflared 설정

| 항목 | 경로 |
|------|------|
| 설정 파일 | `/etc/cloudflared/config.yml` |
| 터널 ID | `a872c44e-f9fb-4e1d-9cc4-13fd9fffb323` (`green-office` 터널) |
| Origin 인증서 | `~/.cloudflared/cert.pem` |
| 터널 자격증명 | `~/.cloudflared/a872c44e-f9fb-4e1d-9cc4-13fd9fffb323.json` |
| 백업 (gitignored) | `.secret/cloudflared-cert.pem`, `.secret/cloudflared-tunnel-credentials.json` |
| systemd 서비스 | `cloudflared.service` (enabled, auto-start) |

> `.secret/` 디렉토리는 `.gitignore`에 등록되어 레포에 올라가지 않는다. 서버 이전 / 재구축 시 수동 복원 대비 사본.

### 서비스 관리 명령

```bash
# 상태 확인
sudo systemctl status cloudflared

# 로그 (실시간)
sudo journalctl -u cloudflared -f

# 재시작 / 정지
sudo systemctl restart cloudflared
sudo systemctl stop cloudflared

# 설정 변경 후 적용 (config.yml 수정했을 때)
sudo systemctl restart cloudflared

# 터널 연결 상태 / 메트릭
curl -s http://127.0.0.1:20241/metrics | grep cloudflared_tunnel
```

### 터널 재구축 (참고)

서버 새로 세팅할 때 순서만 기록.

```bash
# 1) 인증 (브라우저로 Cloudflare 계정 로그인)
cloudflared tunnel login

# 2) 터널 생성 (이미 있으면 생략)
cloudflared tunnel create green-office

# 3) /etc/cloudflared/config.yml 작성 (위 표의 경로들)

# 4) DNS CNAME 자동 등록
cloudflared tunnel route dns green-office green-office.uk
cloudflared tunnel route dns green-office www.green-office.uk
cloudflared tunnel route dns green-office dev.green-office.uk

# 5) systemd 서비스 등록
sudo cloudflared service install
sudo systemctl enable --now cloudflared
```

---

## 로깅

### 구조

애플리케이션 로그는 3가지 파일로 분리 관리된다.

| 파일 | 레벨 | 내용 |
|------|------|------|
| `app.log` | INFO+ | 전체 애플리케이션 로그 |
| `error.log` | WARN+ | 경고 및 오류 전용 |
| `activity.log` | INFO | 사용자 활동 이벤트 전용 |

### 기록되는 활동 이벤트

`activity.log`에는 다음 이벤트가 구조화된 형식으로 기록된다.

| 이벤트 | 태그 | 포함 정보 |
|--------|------|----------|
| 로그인 성공 | `[LOGIN]` | email, IP |
| 로그인 실패 | `[LOGIN_FAILED]` | email, IP (SYSTEM WARN도 함께 기록) |
| 회원가입 | `[REGISTER]` | email, nickname |
| 게시글 작성 | `[POST_CREATE]` | userId, nickname, category, postId |
| 게시글 수정 | `[POST_UPDATE]` | userId, nickname, postId |
| 게시글 삭제 | `[POST_DELETE]` | userId, nickname, postId |
| 출석 체크인 | `[ATTENDANCE]` | userId, nickname, dropsAwarded |
| 출석 거절 | `[ATTENDANCE_DENIED]` | userId, nickname, reason |
| 뽑기 시도 | `[GACHA_DRAW]` | userId, nickname, prize, winner, drawId |
| 뽑기 한도 초과 | `[GACHA_LIMIT]` | userId, nickname |
| 물방울 선물 | `[DROP_GIFT]` | senderId, receiverId, amount |

### 로그 파일 위치

- **Dev 환경**: 컨테이너 내부 `/app/logs/` (볼륨 마운트 없음)
- **Prod 환경**: Docker 볼륨 `logs_prod` → 컨테이너 `/app/logs/`

```bash
# prod 로그 실시간 확인
docker exec greenforest-backend-prod tail -f /app/logs/app.log
docker exec greenforest-backend-prod tail -f /app/logs/activity.log
docker exec greenforest-backend-prod tail -f /app/logs/error.log
```

### 로그 롤링 정책

- 일별 롤링, `archive/` 폴더에 `.gz` 압축 보관
- `app.log`: 최대 30일 / 1GB
- `error.log`: 최대 30일 / 500MB
- `activity.log`: 최대 30일 / 500MB

---

## 백업 (Prod)

### 스크립트

`scripts/backup-prod.sh` — prod DB + 활동 로그를 날짜별 스냅샷으로 저장.

- **DB**: `mysqldump` → gzip 압축 (`db_YYYY-MM-DD.sql.gz`)
- **로그**: `app.log`, `error.log`, `activity.log` + `archive/` → gzip 압축
- **보관**: **14일**, 이전 스냅샷은 자동 삭제

### 수동 실행

```bash
# 프로젝트 루트에서 실행
./scripts/backup-prod.sh
```

기본 저장 경로: `~/backups/green-forest/YYYY-MM-DD/`

환경변수로 오버라이드 가능:
```bash
BACKUP_BASE_DIR=/data/backups ./scripts/backup-prod.sh
```

### 크론 자동화

서버 타임존(Asia/Seoul, KST)이 설정되어 있어 아래 crontab이 **매일 새벽 3시 KST**에 실행된다.
현재 서버에 등록된 crontab (`crontab -l`):

```
0 3 * * * /home/justant/Data/Green-Forest/scripts/backup-prod.sh >> /var/log/greenforest-backup.log 2>&1
```

크론 실행 로그 확인:
```bash
tail -f /var/log/greenforest-backup.log
```

---

## 컨테이너 관리

```bash
# 상태 확인
docker compose -p green-forest-dev  -f docker-compose.dev.yml  ps
docker compose -p green-forest-prod -f docker-compose.prod.yml ps

# 로그
docker logs greenforest-backend-dev -f
docker logs greenforest-backend-prod -f

# 재시작
docker compose -p green-forest-dev  -f docker-compose.dev.yml  restart backend
docker compose -p green-forest-prod -f docker-compose.prod.yml restart backend

# 중지 (데이터 유지)
docker compose -p green-forest-dev  -f docker-compose.dev.yml  down
docker compose -p green-forest-prod -f docker-compose.prod.yml down

# 중지 + 볼륨 삭제 (데이터 초기화 — 절대 prod에서 쓰지 말 것)
docker compose -p green-forest-dev -f docker-compose.dev.yml down -v
```

---

## 기능 이력

### 2026-04-30

#### 관리자 뽑기 기록 조회
- 관리자 페이지 > 뽑기 탭 하단에 "뽑기 기록 조회" 섹션 추가
- 필터: 닉네임, 날짜 범위, 상품별, 당첨만 보기
- 통계: 총 뽑기 수 / 당첨 수 / 실제 당첨률 표시
- 테이블: 날짜, 닉네임, 상품명, 금액, 드랍 소모량, 확률, RNG값, 당첨 여부, 수령 상태
- 페이지네이션 (30건/페이지)
- 관련 파일: `GachaDrawRepository.java`, `GachaService.java`, `GachaController.java`, `frontend/src/app/admin/page.tsx`, `frontend/src/lib/api.ts`, `frontend/src/types/index.ts`

#### 설문/투표 기능
- 게시글 카테고리에 "설문" 타입 추가 (관리자 전용 작성)
- 투표 항목(텍스트/이미지), 마감일, 복수선택, 익명, 공지 옵션 설정 가능
- 사용자 투표 / 결과 실시간 표시 / 진행률 바
- 사용자가 직접 옵션 추가 기능 (allowOptionAddByUser)
- 관리자: 투표 현황 보기(항목별 투표자 목록), 즉시 종료
- 관련 테이블: `surveys`, `survey_options`, `survey_votes`
- 관련 파일: `SurveyView.tsx`, `SurveyCreateForm.tsx`, `api.ts`

#### 식물 성장 점수 로그
- 식물 레벨업 점수 획득 이력 기록 (`growth_score_log` 테이블)
- 점수 획득 이유 enum: `POST`, `COMMENT`, `ATTENDANCE`, `QUEST`, `GIFT_RECEIVED`, `ADMIN`
- 관련 파일: `GrowthScoreLog.java`, `GrowthScoreReason.java`, `GrowthScoreLogRepository.java`, `PlantGrowth.java`

#### 댓글 수 버그 수정
- 게시글 상세 페이지 "댓글 0개" 표시 버그 수정
- 별도 API 호출 제거, 트리 구조 재귀 카운트(`countComments`)로 정확히 집계
- 관련 파일: `frontend/src/components/CommentSection.tsx`

#### 관리자 뽑기 기록 — 상품별 필터 추가
- 뽑기 기록 조회에 상품 선택 드롭다운 추가 (기존: 닉네임/날짜/당첨만 → 추가: 상품별)
- 상품 목록은 기존 `prizes` state 재사용, 추가 API 호출 없음
- 관련 파일: `GachaDrawRepository.java`, `GachaService.java`, `GachaController.java`, `api.ts`, `admin/page.tsx`

#### 출석 추첨 중복 당첨 버그 수정
- **원인:** `@EnableWebSocketMessageBroker`가 생성하는 `messageBrokerTaskScheduler` (pool=4) 빈이 Spring Boot `TaskSchedulingAutoConfiguration`의 `@ConditionalOnMissingBean(TaskScheduler.class)` 조건을 막아, `@Scheduled` 태스크 전체가 WebSocket 4-스레드 스케줄러를 공유하게 됨. 11:00:00에 `AttendanceScheduler`와 `EventScheduler`가 동시 실행되면서 `drawDailyWinner`가 두 번 호출되어 당첨자가 2명 선정됨.
- **수정 1:** `config/SchedulingConfig.java` 신규 추가 — `"taskScheduler"` 이름으로 단일 스레드(pool=1) 전용 스케줄러 등록. `ScheduledAnnotationBeanPostProcessor`가 이름 기준으로 이 빈을 우선 선택하여 WebSocket 스케줄러와 분리됨.
- **수정 2:** `service/AttendanceDrawCoordinator.java` 신규 추가 — `ReentrantLock`으로 추첨 진입점 직렬화. 스케줄러와 관리자 API 어느 경로로 호출되어도 동시 실행 차단.
- 관련 파일: `SchedulingConfig.java`, `AttendanceDrawCoordinator.java`, `AttendanceScheduler.java`, `AdminController.java`

#### DB 자동 백업 크론 등록
- `scripts/backup-prod.sh` 보관 기간 30일 → **14일** 변경
- 서버 crontab에 `0 3 * * *` (KST 매일 새벽 3시) 자동 실행 등록
- 로그: `/var/log/greenforest-backup.log`
