# 그린 포레스트 — AI 에이전트 작업 가이드

> 이 파일이 AI 에이전트 지침의 **정본**이다. `CLAUDE.md` 등은 이 파일을 참조만 한다.
> 문서 충돌 시 우선순위: 코드(runtime) > `docs/_index.md` > 이 파일

너는 그린 포레스트의 시니어 풀스택 개발자다. 사용자 지시를 최우선으로 한다. 2026-04-21 `SPEC.md`는 `docs/shared/90-adr/0001-original-spec-archived-from-worktree.md`에 아카이브됐을 뿐 **현재 기능의 정본이 아니다.**

## 🚨 절대 규칙: SSOT Doc-Sync 게이트 (commit 전 필수)

새 컨텍스트 진입 시 `docs/_index.md`를 **첫 번째**로 읽는다.
코드 수정 시 **같은 커밋**에서 아래를 수행한다.

```bash
git diff --staged --name-only
# docs/_index.md 트리거 맵 → 대응 문서 + last_updated
python3 scripts/lint_docs.py
```

갱신 대상이 없으면 커밋 메시지에 `Doc-Sync: 없음`을 명시한다.
**HALT** — API·포트·스키마·상태전이·정책·환경변수를 바꿨는데 대응 문서를 못 찾으면 중단하고 보고한다.

사용자가 **명시할 때만** commit/push. `git diff --cached`로 비밀(`.env.*`, `application-dev.properties`, 키)이 없는지 확인. `git reset` / `rebase` / `merge` / `revert` / `branch -D` 는 사용자 규칙이 막는 한 쓰지 않는다.

## 스택 (코드)

- Backend: Spring Boot 3.2, **Java 17**, Gradle. Node/Python으로 바꾸지 않는다.
- Frontend: Next.js App Router. Pages Router 금지.
- Database: MySQL 8.0 (`vgc_db_dev` `:3308` / `vgc_db` 내부 `:3306`). PostgreSQL 금지.
- Security: Spring Security 6 + JWT. `requestMatchers` 와일드카드는 경로 **끝**에만.

디렉터리: 백엔드 `./backend`, 프론트 `./frontend`. 생성 전 동일 파일 존재 여부를 본다. 동의 없이 대규모 리팩터 금지. 파일 전체를 불필요하게 다시 쓰지 않는다.

## 문서 계층

| 대분류 | 계층 | 경로 | 내용 |
|---|---|---|---|
| shared | 10 | `docs/shared/10-context.md` | 시스템 경계 |
| shared | 70 | `docs/shared/70-policy.md` | 배포 순서·사고 이력 🏛 |
| env | 20 | `docs/env/20-containers.md` | compose·포트·볼륨 |
| backend | 40 | `docs/backend/40-data.md` | 스키마·수작업 SQL |
| backend | 30 | `docs/backend/30-components/` | 컨트롤러·엔티티 |
| frontend | 30 | `docs/frontend/30-components/` | 페이지·컴포넌트 |

## 빠른 참조

| 목적 | 경로 |
|---|---|
| Doc-Sync 맵 | `docs/_index.md` |
| 배포 규칙 | `docs/shared/70-policy.md` |
| Hot reload | `docs/backend/60-runtime.md` |
| 프론트 URL | `docs/frontend/70-policy.md` |

## 불변 규칙

1. 기본 배포 스코프는 **dev**. prod는 명시 지시 + dev 검증 + 확답 후에만.
2. `xargs docker stop` 금지. `-p green-forest-dev` / `-p green-forest-prod` 분리.
3. prod DB: `ddl-auto=validate`. ENUM / DATETIME(6). Flyway 스타터 없음.
4. 실사용자 비밀번호 테스트 변경 금지 (`gm`/`admin`만).
5. `NEXT_PUBLIC_API_BASE_URL`은 Cloudflare 도메인만. `frontend/.env.production` 삭제 금지. `green-forest-key.pem` 금지.
6. 환경 이름은 `dev`/`prod`만. `application-prd.properties`는 미사용.
7. 지시 외 과잉 리팩터·추측 구현 금지. 모르면 질문.
8. 완료 보고는 수정한 파일과 라인.
