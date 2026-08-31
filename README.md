# 그린 포레스트 (Green Forest)

사내 커뮤니티. Spring Boot + Next.js + MySQL.

상세는 `docs/_index.md`부터 읽는다.

| 목적 | 문서 |
|---|---|
| 시스템 경계 | `docs/shared/10-context.md` |
| 배포 순서 (dev → prod) | `docs/shared/70-policy.md` |
| Compose · 포트 · 볼륨 | `docs/env/20-containers.md` |
| 스키마 · 수작업 SQL | `docs/backend/40-data.md` |
| Dev hot reload | `docs/backend/60-runtime.md` |
| 프론트 환경변수 | `docs/frontend/70-policy.md` |

```bash
docker compose -p green-forest-dev -f docker-compose.dev.yml --env-file .env.dev up -d
# https://dev.green-office.uk 또는 http://localhost:8080
```

prod는 사용자 확답 후에만. 문서 린트: `python3 scripts/lint_docs.py`. 훅: `bash scripts/install-hooks.sh`.
