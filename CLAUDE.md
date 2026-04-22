# 그린 포레스트 - Claude Instructions

## 🎯 Role
너는 그린 포레스트 프로젝트의 시니어 풀스택 개발자다.
나(사용자)의 지시를 최우선으로 하며, 기획서(`SPEC.md`)를 벗어나지 않는다.

## 🛠 Stack Enforcement
- **Backend:** Spring Boot (Java 21, Gradle). 절대 Node.js나 Python으로 바꾸지 마라.
- **Frontend:** Next.js (App Router). Pages Router 사용 금지.
- **Database:** MySQL 8.0 (Dev DB: `vgc_db_dev` port 3308 / Prod DB: `vgc_db` port 3306 내부). PostgreSQL로 바꾸지 마라.
- **Security:** Spring Security 6 + JWT.

## 📝 Coding Rules
- **Incremental Updates:** 코드를 수정할 때 전체 파일을 다시 쓰지 말고, 변경이 필요한 부분만 수정해라.
- **No Refactoring without Permission:** 로직 최적화라며 기존 구조를 뒤엎지 마라. 수정 전 반드시 동의를 구해라.
- **Strict Adherence to SPEC.md:** 모든 기능 구현은 `SPEC.md`의 요구사항을 1순위로 따른다. 임의로 기능을 추가하거나 생략하지 마라.

## ⚠️ Spring Security & Pattern Rules
- **Path Pattern:** Spring Security 6의 `requestMatchers` 패턴 문법을 엄격히 준수해라.
- 와일드카드(`**`)는 반드시 경로의 마지막에 배치해라.
- 중간 경로에 `*`를 남발하여 `PatternParseException`을 유발하지 마라.

## 📁 Directory Structure
- 모든 백엔드 코드는 `./backend` 폴더 아래에 생성한다.
- 모든 프론트엔드 코드는 `./frontend` 폴더 아래에 생성한다.
- 파일 생성 전, 해당 경로에 동일한 파일이 있는지 먼저 체크해라.

## 💬 Communication
- 지시받은 내용 외에 "더 좋게 바꿨어요" 식의 과잉 친절은 금지한다.
- 작업을 완료하면 어떤 파일의 어느 라인을 수정했는지 요약해서 보고해라.
- 모르는 부분이 생기면 추측해서 짜지 말고 나에게 질문해라.
## 🚨 Dev → Prod 배포 순서 (절대 규칙)
- **기본 스코프는 항상 dev.** 사용자가 "바꿔줘 / 수정해줘 / 변경해줘 / 적용해줘" 등의 지시를 하면 **dev 환경에만 적용한다.** prod는 건드리지 않는다.
- **prod는 사용자가 "prod", "운영", "실서버" 등을 명시적으로 언급했을 때만 변경 대상이 된다.** 암묵적·자동 prod 반영 절대 금지.
- **이미지/컨테이너 변경은 반드시 dev → 테스트 완료 → prod 순서로 진행한다.** 순서 위반 금지.
- **dev와 prod를 동시에 빌드/재배포하는 것은 금지.** (예: `docker compose -f ... up --build` 를 prod/dev 양쪽 한 턴에 실행 금지)
- prod에 적용하기 **직전에 반드시 사용자에게 "dev에 먼저 적용하고 테스트 끝났습니까?" 질문하고 확답을 받아야 한다.** 확답 없이 prod 변경 불가.
- dev에서 테스트가 실패했거나 미완료라면 **절대 prod에 동일 변경을 적용하지 마라.**
- 대상: Docker 이미지 빌드, 컨테이너 recreate, `.env.prod` 값 변경 적용, nginx/cloudflared 설정 반영 등 prod 사용자 트래픽에 영향을 주는 모든 배포.
- 예외: prod에만 존재하는 긴급 롤백(이전 이미지로 되돌리기), prod DB 긴급 대응 등은 사용자 지시를 명시적으로 받은 경우에 한해 허용.

## 🔴 Docker 컨테이너/이미지 조작 안전 수칙 (재발 방지)

**사고 경위 (2026-04-21):** `docker ps | xargs docker stop` 광범위 명령으로 prod 컨테이너가 함께 중단됨. dev 이미지 빌드로 prod 이미지 태그가 덮어씌워져 prod backend가 schema-validation 오류로 재시작 루프에 빠짐.

- **`xargs docker stop/rm` 광범위 명령 절대 금지.** 컨테이너 조작 시 반드시 특정 컨테이너 이름을 명시해라. 예: `docker stop greenforest-backend-dev` (O) / `docker ps | xargs docker stop` (X)
- **dev와 prod는 반드시 별도 이미지 태그를 사용해야 한다.** `docker compose build` 시 `-p green-forest-dev` / `-p green-forest-prod` 프로젝트명을 명시하여 이미지 태그가 분리되도록 해라. 같은 `latest` 태그를 공유하는 상태를 방치하지 마라.
- **`docker stop/rm/restart` 명령 실행 전, 반드시 `docker ps --filter name=...` 으로 대상 컨테이너만 나열한 후 사용자에게 확인을 받아라.** prod 컨테이너가 목록에 포함되어 있으면 즉시 중단하고 사용자에게 보고해라.
- **prod 이미지를 빌드할 때는 `-p green-forest-prod` 프로젝트명을 항상 명시해라.** 프로젝트명 없이 빌드하면 기본 태그가 prod와 겹칠 수 있다.
- **prod DB는 `validate` DDL 모드이므로 새 엔티티 추가 시 반드시 DDL SQL을 prod DB에 직접 적용해야 한다.** dev DB에서 `mysqldump --no-data`로 스키마를 추출하여 prod에 적용하는 방식을 표준으로 사용해라.

## 📦 환경 파일 규약 (local 금지)
- **환경 구분 이름은 `dev` / `prod` 두 가지만 사용한다.** `local` 이라는 이름은 더 이상 쓰지 않는다.
- **Docker Compose 파일:** `docker-compose.dev.yml`, `docker-compose.prod.yml`
- **환경 변수 파일:** `.env.dev`, `.env.prod` (루트) / `frontend/.env.dev` (프론트)
- **Spring Profile:** `dev`, `prod` — `SPRING_PROFILES_ACTIVE: dev` 등으로 지정
- **Spring Boot 설정 파일:** `application-dev.properties`, `application-prod.properties`
- **Compose project name:** `green-forest-dev`, `green-forest-prod` (항상 `-p` 로 명시)
- 볼륨 이름도 compose 파일에 `name:` 으로 고정되어 있다. 볼륨명을 임의로 바꾸지 마라 (기존 데이터 연결이 끊긴다).

## ⚡ Dev Hot Reload (재빌드 불필요)

dev 환경은 `Dockerfile.dev` + 소스 bind-mount + `bootRun`/`next dev` 조합으로 **코드 저장만으로 1~5초 내 반영된다.** prod는 기존대로 `bootJar` + `next build` 유지 (건드리지 마라).

**재빌드 없이 저장만으로 반영되는 변경:**
- `backend/src/**` Java 파일 → Gradle `compileJava --continuous` 가 재컴파일 (~1s) → `spring-boot-devtools` 가 Spring context 재시작 (총 2~5s)
- `frontend/src/**`, `frontend/public/**`, `frontend/tailwind.config.ts`, `frontend/postcss.config.js` → `next dev` HMR (1~2s, 브라우저 자동 새로고침)
- `application-dev.properties` (볼륨 마운트 시)

**재빌드가 필요한 변경 (이미지 rebuild 필요):**
- `backend/build.gradle` 의 **의존성** 추가·변경 → `docker compose ... up -d --build backend`
- `frontend/package.json` 의 **의존성** 추가·변경 → `docker compose ... up -d --build frontend`
- `Dockerfile.dev` 자체 변경 → 해당 서비스만 rebuild
- dev용 **새 볼륨 마운트** 추가 시 → `down` 후 `up` (recreate 필요)

**UI가 깨져 보이거나 CSS/레이아웃이 이상할 때 (stale cache):**
`.next` anonymous volume 에 broken build cache 가 남아 있을 수 있다. frontend 컨테이너를 제거하고 재기동하면 캐시가 리셋된다.
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev stop frontend
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev rm -f frontend
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev up -d --no-deps frontend
```
브라우저는 **Ctrl+Shift+R** 로 하드 리프레시.

**원칙:** 코드만 고칠 때는 `--build` 를 쓰지 마라. 불필요한 45~90초 대기가 생긴다. 의존성이나 Dockerfile.dev 를 만진 경우에만 rebuild.

## 🌐 프론트엔드 환경변수 & API URL 규칙

**prod 는 `NEXT_PUBLIC_*` 를 빌드 타임에 JS 번들에 주입한다.** 값이 바뀌면 반드시 `docker compose -p green-forest-prod ... up -d --build frontend` 로 재빌드해야 한다.

**dev 는 `next dev` + `env_file: ./frontend/.env.dev` 런타임 주입 구조라 재빌드 없이 컨테이너 restart 만으로 반영된다.**
```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml -p green-forest-dev restart frontend
```

- **`frontend/.env.dev`의 `NEXT_PUBLIC_API_BASE_URL`은 반드시 Cloudflare 도메인을 사용해야 한다.**
  - dev: `https://dev.green-office.uk/api`
  - prod: `https://green-office.uk/api`
- **서버 IP 주소나 내부 포트(예: `192.168.x.x:8080`)를 직접 쓰지 마라.** 브라우저는 서버의 localhost가 아니므로 IP가 바뀌거나 방화벽에 막히면 API 전체가 무응답된다.
- **사고 사례 (2026-04-21):** IP가 `100.99.33.127 → 192.168.45.240`으로 바뀐 후 `.env.dev`를 미갱신한 채 재빌드 → 브라우저에서 글/이미지/랭킹 전부 로딩 안 됨. 도메인 기반 URL로 전환 후 해결.

## 📌 Git Commit / Push 규칙
- **사용자가 명시적으로 지시할 때만 commit/push를 수행한다.** 지시 없이 자동 커밋 금지.
- 작업 완료 후에는 변경 파일 요약과 함께 스테이징/커밋 제안을 먼저 한다.
- 커밋 전에는 반드시 민감정보(비밀번호, 토큰, `.env.*`, `application-*.properties` 등)가 포함되지 않는지 `git diff --cached` 로 검증한다.
- `git reset`, `git rebase`, `git merge`, `git revert`, `git branch -D` 등 파괴적 명령은 `.claude/settings.local.json` 의 deny 규칙으로 차단되어 있다.
