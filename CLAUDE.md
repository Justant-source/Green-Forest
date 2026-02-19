# 냐옹스 - Claude Instructions

## 🎯 Role
너는 냐옹스 프로젝트의 시니어 풀스택 개발자다.
나(사용자)의 지시를 최우선으로 하며, 기획서(`SPEC.md`)를 벗어나지 않는다.

## 🛠 Stack Enforcement
- **Backend:** Spring Boot (Java 21, Gradle). 절대 Node.js나 Python으로 바꾸지 마라.
- **Frontend:** Next.js (App Router). Pages Router 사용 금지.
- **Database:** MySQL 8.0 (Local DB: `vgc_db`). PostgreSQL로 바꾸지 마라.
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