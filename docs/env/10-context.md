---
title: L1 Context — env (인프라) 경계
last_updated: 2026-08-31
---

# env/10-context

> 저장소에 `env/` 디렉토리는 **없다.** compose·nginx는 루트에 있다. 이 대분류는 그 인프라 파일의 문서 폴더다.

---

## §1. 경계

포함: `docker-compose.dev.yml`, `docker-compose.prod.yml`, `nginx/nginx.dev.conf`, `nginx/nginx.prod.conf`, 루트 `.env.dev` / `.env.prod` (gitignore).

포함하지 않음: Spring `application-*.properties`, Next 소스.

---

## §2. 액터

| 액터 | 접점 |
|---|---|
| 개발자 | `-p green-forest-dev` |
| 운영자 | `-p green-forest-prod` |
| Cloudflare Tunnel | 호스트 nginx `:8080`(dev) / `:80`(prod) |

토폴로지·포트·볼륨은 `docs/env/20-containers.md`.
