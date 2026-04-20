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
│    docker compose -f        │
│    docker-compose.local.yml │
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
┌─────────────────────────────┐
│ 4. prod에 동일 변경 적용     │
│    docker compose -f        │
│    docker-compose.prod.yml  │
│    up -d --build <svc>      │
└─────────────────────────────┘
```

---

## 환경 구성

### 전제 조건
- Docker Desktop 설치
- 프로젝트 루트에 환경변수 파일 준비

```bash
cp .env.local.example .env.local   # 값 채우기
cp .env.prod.example  .env.prod    # 값 채우기 (서버에서)
```

> ⚠️ **`NEXT_PUBLIC_*` 값 변경 시 프론트엔드 재빌드 필수.** Next.js는 이 변수들을 **빌드 타임에 JS 번들로 주입**하기 때문에 컨테이너 restart만으로는 반영 안 됨.
> ```bash
> docker compose -f docker-compose.prod.yml  --env-file .env.prod  up -d --build frontend
> docker compose -f docker-compose.local.yml --env-file .env.local up -d --build frontend
> ```
> 백엔드 CORS(`CORS_ORIGINS`)는 런타임 환경변수라 `--force-recreate`만 하면 반영된다.

---

## Local (개발) 환경

### 실행

```bash
docker compose -f docker-compose.local.yml --env-file .env.local up --build
```

### 접속 주소

| 서비스 | 주소 |
|--------|------|
| 웹 (Nginx) | http://localhost:8080 |
| Backend API | http://localhost:8080/api |
| Backend 직접 | http://localhost:9091 |
| MySQL (외부) | localhost:**3308** |

### DB 정보

| 항목 | 값 |
|------|----|
| Host | localhost |
| Port | **3308** |
| Database | `vgc_db_dev` |
| Username | root |
| Password | `.env.local`의 `DB_PASSWORD` |

### MySQL 클라이언트 접속 예시

```bash
mysql -h 127.0.0.1 -P 3308 -u root -p vgc_db_dev
```

---

## Prod (운영) 환경

### 실행

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

### 접속 주소

| 서비스 | 주소 |
|--------|------|
| 웹 (공개) | https://green-office.uk, https://www.green-office.uk |
| Dev 공개 | https://dev.green-office.uk (Local 환경에 연결) |
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

| 항목 | Local | Prod |
|------|-------|------|
| Spring Profile | `local` | `prod` |
| DB 이름 | `vgc_db_dev` | `vgc_db` |
| MySQL 외부 포트 | **3308** | 미노출 |
| Backend 외부 포트 | **9091** | 미노출 |
| Nginx 포트 | **8080** | **80** |
| DDL 정책 | `update` | `validate` |
| SQL 로그 | ON | OFF |
| 컨테이너 이름 prefix | `greenforest-*-dev` | `greenforest-*-prod` |

---

## DB 마이그레이션 (Prod → Local, 최초 1회)

```bash
# 1. prod 서버에서 덤프
docker exec greenforest-mysql-prod \
  mysqldump -uroot -p${DB_PASSWORD} vgc_db > dump.sql

# 2. dump.sql을 local 머신으로 복사 후 복원
docker exec -i greenforest-mysql-dev \
  mysql -uroot -p${DB_PASSWORD} vgc_db_dev < dump.sql
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
| `dev.green-office.uk` | Local/Dev | `greenforest-nginx-dev` | `http://localhost:8080` |

### nginx 설정 분리

prod / dev 컨테이너가 각자의 `server_name`을 갖도록 파일을 분리.

| 파일 | 용도 | 마운트 대상 |
|------|------|--------------|
| `nginx/nginx.prod.conf` | `server_name green-office.uk www.green-office.uk` | `docker-compose.prod.yml` |
| `nginx/nginx.dev.conf`  | `server_name dev.green-office.uk` | `docker-compose.local.yml` |

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

## 컨테이너 관리

```bash
# 상태 확인
docker compose -f docker-compose.local.yml ps
docker compose -f docker-compose.prod.yml  ps

# 로그
docker logs greenforest-backend-dev -f
docker logs greenforest-backend-prod -f

# 재시작
docker compose -f docker-compose.local.yml restart backend
docker compose -f docker-compose.prod.yml  restart backend

# 중지 (데이터 유지)
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.prod.yml  down

# 중지 + 볼륨 삭제 (데이터 초기화)
docker compose -f docker-compose.local.yml down -v
```
