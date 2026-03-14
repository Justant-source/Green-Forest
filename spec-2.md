# 🌿 그린 포레스트 웹앱 — 기능 분석 및 Claude Code 작업 지시서

## 문서 목적
이 문서는 github.com/dalkong10300/vgc (고양이 집사 커뮤니티)를 하드포크하여
"그린 포레스트" (반려식물 RPG 커뮤니티)로 전환하기 위한 종합 기획서이다.
Claude Code가 이 문서 하나만 읽고 전체 작업을 수행할 수 있도록 작성되었다.

---

# PART 1. 기능 분류 — 필수 vs 추천

## A. 필수 기능 (Must-Have)

프로젝트 시나리오가 정상 작동하려면 반드시 있어야 하는 기능들이다.

### A-1. 사용자 인증 및 프로필

| 기능 | 상세 설명 |
|------|-----------|
| **회원가입/로그인** | 이메일 또는 소셜 로그인. 20명 한정 폐쇄형 커뮤니티. |
| **프로필 설정** | 닉네임, 프로필 사진, 나의 식물 이름, 식물 종류(4종 중 택1), 직업군(식물 선택 시 자동 배정), 파티 소속 표시(운영자가 지정해줌) |
| **마이 가든 (마이페이지)** | 나의 식물 정보 카드(이름, 종류, 직업군, 속성), 누적 물방울, 월별 획득 이력, 내가 작성한 글 목록. |

### A-2. 피드/게시판 시스템 (탭 구조)

앱의 메인 화면은 하단 탭 네비게이션으로 구성되며, 총 5개 탭 + 우상단 프로필이다.

| 탭 | 이름 | 기능 |
|----|------|------|
| **탭1 (홈)** | 숲의 광장 | 모든 게시글(긍정문구 + 동료칭찬 + 퀘스트)이 통합된 타임라인 피드. 최신순 정렬. 사진+글 카드 형태.  |
| **탭2** | 긍정 문구 | "긍정 문구" 카테고리로 태그된 글만 필터링하여 표시. 일일 퀘스트 전용 피드. |
| **탭3** | 동료 칭찬 | "동료 칭찬" 카테고리로 태그된 글만 필터링하여 표시. 주간 퀘스트 전용 피드. 물방울 선물 기능 포함. |
| **탭4** | 퀘스트 | 월간 퀘스트 (야외에서 사진), 파티 퀘스트, 이벤트 퀘스트 등 특별 퀘스트 인증 피드. GM이 퀘스트를 생성하고 참가자가 인증글을 올리는 구조. |
| **탭5** | 포레스트 랭킹 | 파티별 물방울 합산 순위 리더보드 (메인). 파티를 클릭하면 해당 파티원들의 개인 물방울 순위를 볼 수 있음. |
| **우상단** | 마이 가든 | 개인 프로필, 나의 식물 정보, 직업군, 누적 물방울, 내 글 목록. |

### A-3. 게시글 작성 (CRUD)

| 기능 | 상세 설명 |
|------|-----------|
| **글 작성** | 제목(선택), 본문(필수), 사진 첨부(선택, 최대 4장), 카테고리 선택(필수: 긍정문구/동료칭찬/퀘스트), 동료 태깅(선택), 익명 여부 토글. |
| **카테고리 선택** | 글 작성 시 반드시 1개의 카테고리를 선택해야 한다. 카테고리에 따라 해당 탭에 노출된다. |
| **사진 업로드** | 이미지 업로드 + 리사이징 + 저장. S3 또는 Firebase Storage 등 외부 스토리지 사용. |
| **동료 태깅** | 글 작성 시 `@닉네임` 형태로 동료를 태깅할 수 있다. 태깅된 동료에게는 자동으로 물방울 5가 지급된다. |
| **익명 작성** | 토글 ON 시 닉네임 대신 "익명의 그린메이커"로 표시. 물방울은 정상 지급. |
| **수정/삭제** | 본인 글만 수정·삭제 가능. |
| **댓글** | 게시글 하단에 댓글 작성 가능. 댓글에도 태깅 가능. |

### A-4. 물방울 포인트 자동 지급 시스템

이것이 이 앱의 가장 핵심적인 백엔드 로직이다. 해당 policy는 json 파일로 관리해서 변경사항에 쉽게 대응할 수 있어야 한다. **CA(관리자)가 수동으로 포인트를 계산하면 안 된다.**

| 트리거 | 지급 물방울 | 중복 제한 | 로직 설명 |
|--------|-----------|----------|-----------|
| "긍정 문구" 카테고리 글 작성 | 10 | 1일 1회 | 같은 날(00:00~23:59) 해당 카테고리로 2번째 글을 올려도 물방울은 첫 1회만 지급. |
| "동료 칭찬" 카테고리 글 작성 | 30 | 주 1회 | 같은 주(월~일) 해당 카테고리로 2번째 글을 올려도 물방울은 첫 1회만 지급. |
| "퀘스트" 카테고리 글 작성 | 50 (기본값, GM이 퀘스트별로 설정 가능) | 퀘스트당 1회 | 각 퀘스트에 대해 1인 1회만 인정. |
| 태깅 당한 경우 | 5 (태깅된 사람에게 지급) | 글당 1인 1회 | A가 B를 태깅하면 B에게 5 지급. 같은 글에서 같은 사람을 여러 번 태깅해도 1회만. |
| GM 수동 지급 | 가변 | 없음 | GM(관리자)이 특정 유저에게 물방울을 직접 지급/차감할 수 있는 관리 기능. 이벤트 보상, 돌발 퀘스트 등에 사용. |

#### 물방울 지급 자동화 핵심 규칙

```
규칙 1: 물방울은 글 작성 완료(저장) 시점에 서버에서 자동 계산하여 즉시 지급한다.
규칙 2: 중복 제한 판정은 서버 사이드에서 수행한다 (클라이언트 조작 방지).
규칙 3: 지급된 물방울은 즉시 개인 누적 잔고와 파티 합산 점수에 모두 반영한다.
규칙 4: 모든 물방울 지급/차감 이력은 트랜잭션 로그로 기록한다 (누가, 언제, 왜, 얼마나).
규칙 5: 식물 난이도 가중치(쉬움×1.0, 보통×1.1, 어려움×1.2)를 지급 시 자동 적용한다.
```

### A-5. 리더보드 (포레스트 랭킹)

| 기능 | 상세 설명 |
|------|-----------|
| **파티별 랭킹 (메인 뷰)** | 5개 파티의 물방울 합산 순위를 1~5위로 표시. 파티명, 파티원 수, 합산 물방울, 순위 변동(전월 대비) 표시. |
| **파티 상세 (서브 뷰)** | 파티를 탭하면 해당 파티원들의 개인 물방울 순위 리스트가 펼쳐진다. 프로필 사진, 닉네임, 직업군 아이콘, 개인 물방울 표시. |
| **실시간 갱신** | 물방울 지급 시 리더보드가 즉시 업데이트된다. (폴링 또는 실시간 구독) |
| **기간 필터** | "이번 달" / "전체 기간" 토글로 조회 기간 전환 가능. |

### A-6. 관리자(GM) 기능

| 기능 | 상세 설명 |
|------|-----------|
| **퀘스트 생성/관리** | GM이 퀘스트를 생성한다: 퀘스트명, 설명, 보상 물방울, 기간(시작~종료), 대상(전체/파티별), 최대 참가 횟수. |
| **물방울 수동 지급/차감** | 특정 유저 또는 파티 전원에게 물방울을 직접 지급하거나 차감. 사유 입력 필수. |
| **유저 관리** | 유저 목록 조회, 파티 배정/변경, 식물·직업군 정보 수정, 계정 비활성화. |
| **파티 관리** | 파티 생성/수정/삭제, 파티원 이동, 파티명 변경. |
| **공지사항** | 숲의 광장(탭1) 상단에 고정되는 공지 게시글 작성. 돌발 이벤트 경보 등에 활용. |
| **통계 대시보드** | 일별/주별/월별 글 작성 수, 활성 유저 수, 물방울 발행 총량, 파티별 활동량 등 요약 통계. |

### A-7. 알림(Notification)

| 알림 유형 | 발생 조건 |
|-----------|-----------|
| 태깅 알림 | 누군가 나를 태깅한 글을 작성했을 때 |
| 댓글 알림 | 내 글에 댓글이 달렸을 때 |
| 물방울 지급 알림 | 물방울이 지급되었을 때 (글 작성 보상, 태깅 보상, GM 수동 지급) |
| 퀘스트 알림 | 새로운 퀘스트가 생성되었을 때, 퀘스트 마감 임박 시 |
| 위해 요소 알림 | 월말 최하위/최상위 참가자에게 자동 발송 |

---

## B. 추천 기능 (Nice-to-Have)

MVP 이후에 추가하면 프로젝트의 재미와 몰입도를 높일 수 있는 기능들이다.

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| **물방울 선물** | 동료 칭찬 탭에서 특정 동료에게 내 물방울을 선물할 수 있는 기능. "동료에게 물방울 보내기" 버튼. | 필수 |
| **투표 기능** | 퀘스트 탭에서 GM이 투표형 퀘스트를 생성 가능 (예: 네이밍 투표, 베스트 인증샷 투표). 참가자가 선택지에 투표하면 자동 집계. | 필수 |
| **1:1 대화(DM)** | 유저 간 1:1 메시지 기능. 5월 감사 메시지 교환 퀘스트 등에 활용. | 필수 |
| **리액션(이모지)** | 게시글에 좋아요 외에 식물 테마 이모지 리액션 (물방울, 햇빛, 새싹, 꽃 등). | 필수 |
| **월말 위해 요소 자동 판정** | 매월 말일에 자동으로 최하위/최상위 참가자를 판별하고 알림을 보내며, 해제 미션(인증샷) 완료 시 자동 처리. | 필수 |
| **보상 상점** | 물방울을 사용하여 보상을 교환 신청하는 인앱 상점. GM이 상품 등록, 유저가 교환 신청, GM이 수동 승인. | 필수 |
| **푸시 알림** | PWA(Progressive Web App) 기반 모바일 푸시 알림. | 필수 |

---

# PART 2. 하드포크 전환 가이드 — VGC → 그린 포레스트

## 원본 프로젝트 (dalkong10300/vgc) 구조 추정

원본은 고양이 집사 전용 커뮤니티로, 아래와 같은 기능이 있는 것으로 추정된다.

| 원본 기능 | 설명 |
|-----------|------|
| 회원가입/로그인 | 이메일 또는 소셜 로그인 |
| 게시판 (입양/분양/홍보 탭) | 카테고리별 게시글 피드 |
| 글 작성 (사진+글) | 사진 업로드, 본문 작성 |
| 댓글 | 게시글에 댓글 달기 |
| 1:1 대화(DM) | 유저 간 1:1 채팅 |
| 프로필 | 닉네임, 프로필 사진 등 |
| 알림 | 댓글, DM 알림 |

## 전환 맵: 가져올 것 / 수정할 것 / 삭제할 것 / 새로 만들 것

### 가져올 것 (Keep As-Is)

원본의 기본 인프라와 공통 기능을 그대로 활용한다.

| 원본 기능 | 그린 포레스트에서의 용도 | 비고 |
|-----------|----------------------|------|
| 인증 시스템 (회원가입/로그인) | 그대로 사용 | 소셜 로그인이 있으면 유지 |
| DB 스키마 기본 구조 (users, posts, comments) | 기반으로 확장 | 필드 추가 필요 |
| 파일 업로드 (사진) | 식물 사진 업로드에 사용 | 그대로 사용 |
| 댓글 시스템 | 그대로 사용 | 태깅 기능 추가 필요 |
| 알림 시스템 기본 구조 | 기반으로 확장 | 알림 유형 추가 필요 |
| 1:1 대화(DM) | 5월 감사 메시지 등에 활용 | 있으면 유지, 없으면 Nice-to-Have |

### 수정할 것 (Modify)

원본 기능의 뼈대는 유지하되, 그린 포레스트 요구사항에 맞게 변경한다.

| 원본 기능 | 변경 내용 |
|-----------|-----------|
| **게시판 카테고리** | "입양/분양/홍보" → "긍정 문구/동료 칭찬/퀘스트"로 카테고리 변경 |
| **탭 구조** | 원본의 탭 이름·아이콘·순서를 그린 포레스트 5탭 구조로 변경 |
| **프로필 페이지** | "마이 가든"으로 리네이밍. 식물 정보, 직업군, 물방울 잔고 필드 추가. |
| **글 작성 폼** | 카테고리 선택(필수), 동료 태깅(@), 익명 토글 추가 |
| **UI 테마/색상** | 고양이 테마 → 식물/숲 테마. 컬러 팔레트를 그린 계열로 변경. 아이콘을 식물/잎/물방울로 교체. |
| **앱 이름/로고** | VGC → "그린 포레스트". 로고를 숲/나무 컨셉으로 교체. |
| **텍스트/복사** | 모든 고양이 관련 문구를 식물/숲 관련 문구로 교체 |

### 삭제할 것 (Remove)

고양이 커뮤니티에만 해당되는 기능을 제거한다.

| 삭제 대상 | 사유 |
|-----------|------|
| 입양/분양 게시판 로직 | 반려식물 프로젝트에서 입양/분양 개념 불필요 |
| 고양이 품종/나이 등 반려동물 전용 필드 | 식물 전용 필드로 대체 |
| 고양이 관련 UI 에셋 (아이콘, 일러스트, 배너 등) | 식물 테마 에셋으로 교체 |
| 위치 기반 기능 (있을 경우) | 사내 프로젝트이므로 불필요 |
| 외부 공유 기능 (있을 경우) | 보안 사업장이므로 외부 공유 불필요 |

### 새로 만들 것 (Build New)

원본에 없는 기능으로, 새로 개발해야 한다.

| 신규 기능 | 우선순위 | 복잡도 |
|-----------|----------|--------|
| **물방울 포인트 시스템 전체** (자동 지급, 중복 제한, 트랜잭션 로그) | 최상 | 높음 |
| **리더보드** (파티별 랭킹 + 개인 랭킹) | 최상 | 중간 |
| **파티 시스템** (파티 CRUD, 파티원 배정, 파티 합산 점수) | 최상 | 중간 |
| **직업군 시스템** (식물→직업군 자동 매핑, 프로필 표시) | 높음 | 낮음 |
| **퀘스트 관리** (GM이 퀘스트 생성, 유저 인증, 자동 보상) | 높음 | 중간 |
| **태깅 시스템** (글 내 @멘션, 태깅 시 자동 물방울 지급) | 높음 | 중간 |
| **익명 작성** | 중간 | 낮음 |
| **GM 관리 패널** (물방울 수동 지급, 유저 관리, 통계) | 높음 | 중간 |
| **투표 기능** | 중간 | 중간 |

---

# PART 3. Claude Code 작업 지시서

## 프로젝트 컨텍스트

```
프로젝트명: 그린 포레스트 (Green Forest)
베이스: github.com/dalkong10300/vgc 하드포크
목적: AI전략팀 20명의 9개월간 반려식물 RPG 커뮤니티 웹앱
사용자: 20명 폐쇄형 (사내 프로젝트)
운영 기간: 2026년 3월 ~ 12월
핵심 원칙: 물방울 포인트 자동 지급으로 관리자 수작업 제로(0)
```

## 작업 순서 (Phase)

### Phase 0: 원본 분석 및 환경 세팅

```
1. github.com/dalkong10300/vgc 를 클론한다.
2. 프로젝트의 기술 스택을 파악한다 (프론트엔드 프레임워크, 백엔드, DB, 파일 스토리지 등).
3. README.md 또는 package.json / requirements.txt 등을 분석하여 의존성을 파악한다.
4. 로컬에서 실행 가능한 상태로 세팅한다.
5. DB 스키마를 분석하여 기존 테이블 구조를 파악한다.
6. 라우팅 구조를 분석하여 기존 페이지/API 엔드포인트를 파악한다.
7. 분석 결과를 ANALYSIS.md 파일로 정리한다.
```

### Phase 1: DB 스키마 확장

기존 스키마를 유지하면서 다음 테이블/필드를 추가한다.

```sql
-- users 테이블 확장 (기존 필드 유지 + 아래 추가)
ALTER TABLE users ADD COLUMN plant_type ENUM('테이블야자', '스파티필름', '무늬홍콩야자', '오렌지자스민') DEFAULT NULL;
ALTER TABLE users ADD COLUMN plant_name VARCHAR(50) DEFAULT NULL;
ALTER TABLE users ADD COLUMN job_class ENUM('탱커', '힐러', '딜러', '버퍼') DEFAULT NULL;
-- job_class는 plant_type 설정 시 자동 매핑:
-- 테이블야자→탱커, 스파티필름→힐러, 무늬홍콩야자→버퍼, 오렌지자스민→딜러
ALTER TABLE users ADD COLUMN element ENUM('땅', '물', '바람', '불') DEFAULT NULL;
ALTER TABLE users ADD COLUMN difficulty ENUM('쉬움', '보통', '어려움') DEFAULT NULL;
ALTER TABLE users ADD COLUMN exp_multiplier DECIMAL(3,2) DEFAULT 1.00;
-- 쉬움=1.00, 보통=1.10, 어려움=1.20
ALTER TABLE users ADD COLUMN party_id INT DEFAULT NULL;
ALTER TABLE users ADD COLUMN is_admin BOOLEAN DEFAULT FALSE; -- GM 권한
ALTER TABLE users ADD COLUMN total_drops INT DEFAULT 0; -- 누적 물방울

-- parties (파티) 테이블 신규
CREATE TABLE parties (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- posts 테이블 확장 (기존 필드 유지 + 아래 추가)
ALTER TABLE posts ADD COLUMN category ENUM('긍정문구', '동료칭찬', '퀘스트') NOT NULL;
ALTER TABLE posts ADD COLUMN quest_id INT DEFAULT NULL; -- 퀘스트 카테고리인 경우 연결
ALTER TABLE posts ADD COLUMN is_anonymous BOOLEAN DEFAULT FALSE;

-- post_tags (태깅) 테이블 신규
CREATE TABLE post_tags (
  id INT PRIMARY KEY AUTO_INCREMENT,
  post_id INT NOT NULL,
  tagged_user_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_tag (post_id, tagged_user_id), -- 같은 글에서 같은 사람 중복 태깅 방지
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (tagged_user_id) REFERENCES users(id)
);

-- drop_transactions (물방울 거래 내역) 테이블 신규
CREATE TABLE drop_transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  amount INT NOT NULL, -- 양수=지급, 음수=차감
  reason_type ENUM('일일퀘스트', '주간퀘스트', '월간퀘스트', '이벤트퀘스트', '태깅보너스', 'GM수동지급', 'GM수동차감', '선물받음', '선물보냄') NOT NULL,
  reason_detail VARCHAR(500) DEFAULT NULL, -- 상세 사유
  related_post_id INT DEFAULT NULL, -- 어떤 글에서 발생했는지
  related_quest_id INT DEFAULT NULL, -- 어떤 퀘스트에서 발생했는지
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- daily_quest_log (일일/주간/월간 퀘스트 중복 방지 로그) 테이블 신규
CREATE TABLE quest_completion_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  quest_type ENUM('일일', '주간', '월간') NOT NULL,
  category ENUM('긍정문구', '동료칭찬', '야외인증') NOT NULL,
  period_key VARCHAR(20) NOT NULL, -- 일일: '2026-03-18', 주간: '2026-W12', 월간: '2026-03'
  post_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_completion (user_id, quest_type, category, period_key)
  -- 이 UNIQUE KEY가 중복 제한의 핵심. 같은 유저가 같은 기간에 같은 퀘스트를 또 완료하면 INSERT 실패.
);

-- quests (관리자 생성 퀘스트) 테이블 신규
CREATE TABLE quests (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  reward_drops INT NOT NULL DEFAULT 50,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  target_type ENUM('전체', '파티별') DEFAULT '전체',
  target_party_id INT DEFAULT NULL, -- 파티별일 경우
  max_completions_per_user INT DEFAULT 1,
  is_active BOOLEAN DEFAULT TRUE,
  created_by INT NOT NULL, -- GM user_id
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

-- quest_completions (퀘스트 완료 기록) 테이블 신규
CREATE TABLE quest_completions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  quest_id INT NOT NULL,
  user_id INT NOT NULL,
  post_id INT NOT NULL, -- 인증 게시글
  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (quest_id) REFERENCES quests(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (post_id) REFERENCES posts(id)
);

-- votes (투표) 테이블 신규 (Nice-to-Have이지만 4월부터 필요)
CREATE TABLE votes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  quest_id INT NOT NULL, -- 투표형 퀘스트
  user_id INT NOT NULL, -- 투표한 사람
  voted_for_user_id INT DEFAULT NULL, -- 투표 대상 (유저 투표일 경우)
  voted_for_option VARCHAR(200) DEFAULT NULL, -- 투표 대상 (선택지 투표일 경우)
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_vote (quest_id, user_id), -- 1인 1표
  FOREIGN KEY (quest_id) REFERENCES quests(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- notifications (알림) 테이블 신규 또는 확장
CREATE TABLE notifications (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL, -- 알림 받는 사람
  type ENUM('태깅', '댓글', '물방울지급', '퀘스트생성', '퀘스트마감임박', '위해요소', '공지') NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT,
  related_post_id INT DEFAULT NULL,
  related_quest_id INT DEFAULT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

> **주의**: 위 SQL은 MySQL 기준 예시이다. 원본 프로젝트의 DB 종류(PostgreSQL, MongoDB 등)에 맞게 변환해야 한다. MongoDB라면 이 스키마를 컬렉션/도큐먼트 구조로 재설계한다.

### Phase 2: 물방울 자동 지급 백엔드 로직

이것이 가장 중요한 핵심 로직이다.

```
파일 위치 (예시): /server/services/dropService.js (또는 해당 프레임워크의 서비스 레이어)

--- 물방울 자동 지급 서비스 ---

함수: awardDropsForPost(userId, postId, category, taggedUserIds)
  
  입력:
    - userId: 글 작성자 ID
    - postId: 작성된 게시글 ID
    - category: '긍정문구' | '동료칭찬' | '퀘스트'
    - taggedUserIds: 태깅된 유저 ID 배열

  로직:
    1. 유저 정보 조회 (exp_multiplier 가져오기)
    
    2. 카테고리별 처리:
       
       IF category == '긍정문구':
         - period_key = 오늘 날짜 (YYYY-MM-DD)
         - quest_completion_log 에 INSERT 시도 (UNIQUE KEY로 중복 체크)
         - INSERT 성공 시: 물방울 = 10 × exp_multiplier → drop_transactions INSERT → users.total_drops 갱신
         - INSERT 실패(중복) 시: 물방울 미지급, 글은 정상 저장
       
       IF category == '동료칭찬':
         - period_key = 이번 주 (YYYY-Www, ISO 주차)
         - quest_completion_log 에 INSERT 시도
         - INSERT 성공 시: 물방울 = 30 × exp_multiplier → 거래 기록 → 잔고 갱신
         - INSERT 실패(중복) 시: 물방울 미지급, 글은 정상 저장
       
       IF category == '퀘스트':
         - quest_id 로 퀘스트 정보 조회
         - quest_completions 에서 해당 유저의 완료 횟수 확인
         - max_completions_per_user 미만이면:
           물방울 = quest.reward_drops × exp_multiplier → 거래 기록 → 잔고 갱신
           quest_completions INSERT
         - 이미 최대 횟수 달성이면: 물방울 미지급, 글은 정상 저장
    
    3. 태깅 보너스 처리:
       FOR EACH taggedUserId IN taggedUserIds:
         - post_tags 에 INSERT (UNIQUE KEY로 중복 방지)
         - INSERT 성공 시: taggedUserId에게 물방울 5 지급 → 거래 기록 → 잔고 갱신
         - INSERT 실패(중복) 시: 무시
         - 알림 생성 (태깅 알림)
    
    4. 리더보드 캐시 무효화 (또는 실시간 갱신 트리거)
    
    5. 알림 생성 (물방울 지급 알림)

---

함수: getLeaderboard(periodType)
  
  입력: periodType = 'monthly' | 'all_time'
  
  로직:
    IF periodType == 'monthly':
      - drop_transactions에서 이번 달 거래만 집계
    ELSE:
      - users.total_drops 사용
    
    파티별 합산:
      SELECT p.id, p.name, SUM(u.total_drops) as party_total
      FROM parties p
      JOIN users u ON u.party_id = p.id
      GROUP BY p.id
      ORDER BY party_total DESC
    
    파티 내 개인별:
      SELECT u.id, u.nickname, u.job_class, u.total_drops
      FROM users u
      WHERE u.party_id = :partyId
      ORDER BY u.total_drops DESC

---

함수: gmManualAward(adminUserId, targetUserId, amount, reason)
  
  로직:
    - adminUserId가 is_admin=TRUE인지 확인
    - drop_transactions INSERT (reason_type='GM수동지급' 또는 'GM수동차감')
    - users.total_drops 갱신
    - 알림 생성
```

### Phase 3: 프론트엔드 변환

```
--- 3-1. 전역 테마 변경 ---

색상 팔레트 변경:
  Primary: #2D8A4E (숲의 녹색)
  Secondary: #5BB37F (연한 녹색)
  Accent: #4CACDE (물방울 파란색)
  Background: #F5F9F0 (아이보리 그린)
  Text: #1A1A2E (다크 네이비)
  Warning: #E8A838 (가을 주황)
  Danger: #D64545 (경고 빨강)

폰트: 
  한국어: 'Pretendard' 또는 'Noto Sans KR'
  영문: 'Inter' 또는 원본 유지

--- 3-2. 탭 네비게이션 변경 ---

기존 탭 구조를 아래로 교체한다:

탭1 아이콘: 나무(🌳) 또는 home 아이콘 → 라벨: "숲의 광장"
탭2 아이콘: 해(☀️) 또는 smile 아이콘 → 라벨: "긍정 문구"
탭3 아이콘: 하트(💚) 또는 heart 아이콘 → 라벨: "동료 칭찬"
탭4 아이콘: 깃발(🏁) 또는 flag 아이콘 → 라벨: "퀘스트"
탭5 아이콘: 트로피(🏆) 또는 bar-chart 아이콘 → 라벨: "랭킹"

우상단: 프로필 아이콘 → "마이 가든"

--- 3-3. 피드 카드 컴포넌트 ---

각 게시글은 카드 형태로 표시:

┌─────────────────────────────────┐
│ [프로필사진] 닉네임 · 직업군배지 · 시간  │
│ (익명이면: 🌿 익명의 그린메이커 · 시간)   │
├─────────────────────────────────┤
│ [카테고리 태그: 긍정문구 🟢]           │
│                                 │
│ 본문 텍스트...                     │
│                                 │
│ [사진 1~4장 그리드]                 │
│                                 │
│ 태그: @동료1 @동료2                 │
├─────────────────────────────────┤
│ 💧12 획득  ·  💬 3 댓글             │
└─────────────────────────────────┘

--- 3-4. 글 작성 화면 ---

┌─────────────────────────────────┐
│ [X 닫기]     글 쓰기      [등록]    │
├─────────────────────────────────┤
│                                 │
│ 카테고리 선택 (필수):               │
│ [긍정 문구] [동료 칭찬] [퀘스트]      │
│                                 │
│ (퀘스트 선택 시: 드롭다운으로 활성 퀘스트 목록) │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 오늘의 이야기를 적어주세요...     │ │
│ │                             │ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ [📷 사진 추가]  [@ 동료 태깅]       │
│                                 │
│ ☐ 익명으로 작성하기                 │
│                                 │
└─────────────────────────────────┘

--- 3-5. 리더보드 화면 ---

┌─────────────────────────────────┐
│      🏆 포레스트 랭킹              │
│  [이번 달 ▼]  [전체 기간]           │
├─────────────────────────────────┤
│                                 │
│  🥇 1위  파티명A     💧 2,340      │
│  ├ 닉네임1 [탱커] 💧520           │
│  ├ 닉네임2 [힐러] 💧480           │
│  ├ 닉네임3 [딜러] 💧450           │
│  └ 닉네임4 [버퍼] 💧390           │
│                                 │
│  🥈 2위  파티명B     💧 2,100      │
│  ▶ (탭하여 펼치기)                  │
│                                 │
│  🥉 3위  파티명C     💧 1,890      │
│  ▶ (탭하여 펼치기)                  │
│                                 │
│  4위  파티명D        💧 1,650      │
│  5위  파티명E        💧 1,420      │
│                                 │
└─────────────────────────────────┘

--- 3-6. 마이 가든 화면 ---

┌─────────────────────────────────┐
│ [← 뒤로]    마이 가든              │
├─────────────────────────────────┤
│        [프로필 사진]               │
│        닉네임                     │
│        파티명 · 직업군              │
│                                 │
│  ┌───────────────────────────┐  │
│  │ 🌱 나의 식물                │  │
│  │ 이름: 뿌리                  │  │
│  │ 종류: 테이블야자 (⭐ 쉬움)     │  │
│  │ 직업군: 🛡️ 탱커 (Guardian)  │  │
│  │ 속성: 🌍 땅               │  │
│  │ 가중치: ×1.0               │  │
│  └───────────────────────────┘  │
│                                 │
│  💧 누적 물방울: 1,240             │
│  📈 이번 달: +340                 │
│                                 │
│  ─── 내 글 목록 ───              │
│  (내가 작성한 글 리스트)             │
│                                 │
└─────────────────────────────────┘

--- 3-7. GM 관리 패널 ---

일반 유저에게는 보이지 않고, is_admin=TRUE인 유저에게만 접근 가능한 별도 페이지.

URL: /admin (또는 마이 가든 내 "관리자 메뉴" 버튼)

기능:
  1. 퀘스트 관리: 목록 / 생성 / 수정 / 삭제
  2. 물방울 수동 지급: 유저 선택 → 금액 입력 → 사유 입력 → 지급/차감
  3. 유저 관리: 목록 / 파티 변경 / 식물·직업군 수정
  4. 파티 관리: 목록 / 생성 / 파티원 이동
  5. 공지 작성: 숲의 광장 상단 고정 공지
  6. 통계: 일별 글 수, 활성 유저, 물방울 발행 총량 차트
```

### Phase 4: 텍스트/에셋 전환

```
--- 모든 고양이 관련 텍스트를 식물/숲 텍스트로 교체 ---

검색 대상 키워드 → 교체:
  "고양이" → "식물" 또는 "반려식물"
  "집사" → "그린 메이커"
  "입양" → (삭제 또는 "퀘스트"로 대체)
  "분양" → (삭제)
  "홍보" → (삭제)
  "냥" → (삭제)
  "펫" → "식물"
  "반려동물" → "반려식물"

--- 아이콘/이미지 에셋 교체 ---
  
  고양이 관련 아이콘/일러스트 → 식물/잎/나무/물방울 아이콘으로 교체
  앱 아이콘 → 나무 또는 숲 아이콘
  로딩 화면 → 새싹이 자라는 애니메이션 또는 물방울 떨어지는 애니메이션
  빈 상태(empty state) 일러스트 → "아직 숲이 조용해요. 첫 번째 이야기를 남겨보세요!" + 새싹 일러스트
  
--- favicon, og:image, 앱 타이틀 교체 ---
  
  <title>그린 포레스트</title>
  <meta property="og:title" content="그린 포레스트 - 우리 팀의 작은 숲" />
```

### Phase 5: 테스트 체크리스트

```
--- 기능 테스트 ---

[ ] 회원가입/로그인이 정상 동작하는가?
[ ] 프로필에서 식물 종류 선택 시 직업군이 자동 매핑되는가?
[ ] "긍정 문구" 카테고리 글 작성 시 물방울 10이 자동 지급되는가?
[ ] 같은 날 "긍정 문구" 를 2번 올리면 두 번째는 물방울이 지급되지 않는가?
[ ] "동료 칭찬" 카테고리 글 작성 시 물방울 30이 자동 지급되는가?
[ ] 같은 주에 "동료 칭찬"을 2번 올리면 두 번째는 물방울이 지급되지 않는가?
[ ] 글에 동료를 태깅하면 태깅된 동료에게 물방울 5가 지급되는가?
[ ] 같은 글에서 같은 동료를 2번 태깅하면 물방울 5가 1번만 지급되는가?
[ ] 익명 글 작성 시 닉네임이 "익명의 그린메이커"로 표시되는가?
[ ] 익명 글 작성 시에도 물방울이 정상 지급되는가?
[ ] 리더보드에서 파티별 합산 순위가 올바르게 표시되는가?
[ ] 파티를 탭하면 파티원 개인 순위가 펼쳐지는가?
[ ] 물방울 지급 시 리더보드가 즉시 갱신되는가?
[ ] GM이 퀘스트를 생성할 수 있는가?
[ ] 유저가 퀘스트 인증글을 올리면 퀘스트 보상이 자동 지급되는가?
[ ] GM이 특정 유저에게 물방울을 수동 지급/차감할 수 있는가?
[ ] 태깅 알림이 정상 발송되는가?
[ ] 댓글 알림이 정상 발송되는가?
[ ] 탭 네비게이션이 올바르게 동작하는가? (숲의 광장/긍정문구/동료칭찬/퀘스트/랭킹)
[ ] 각 탭에서 해당 카테고리의 글만 필터링되어 표시되는가?
[ ] 마이 가든에서 내 식물 정보, 직업군, 누적 물방울이 올바르게 표시되는가?

--- 경계 테스트 ---

[ ] 물방울 잔고가 음수가 될 수 있는가? (되면 안 됨, 또는 GM 차감 시에만 허용)
[ ] 동시에 2개의 글을 빠르게 올리면 중복 지급이 발생하는가? (발생하면 안 됨)
[ ] 20명 전원이 동시에 글을 올리면 서버가 정상 처리하는가?
[ ] 파티에 속하지 않은 유저의 물방울은 리더보드에 어떻게 처리되는가?
```

---

# PART 4. 식물→직업군 자동 매핑 테이블

이 매핑은 프로필에서 식물을 선택하는 순간 서버 사이드에서 자동 적용되어야 한다.

```javascript
const PLANT_JOB_MAP = {
  '테이블야자': {
    jobClass: '탱커',
    jobClassEn: 'Guardian',
    element: '땅',
    difficulty: '쉬움',
    expMultiplier: 1.00
  },
  '스파티필름': {
    jobClass: '힐러',
    jobClassEn: 'Healer',
    element: '물',
    difficulty: '쉬움',
    expMultiplier: 1.00
  },
  '무늬홍콩야자': {
    jobClass: '버퍼',
    jobClassEn: 'Enchanter',
    element: '바람',
    difficulty: '보통',
    expMultiplier: 1.10
  },
  '오렌지자스민': {
    jobClass: '딜러',
    jobClassEn: 'Striker',
    element: '불',
    difficulty: '어려움',
    expMultiplier: 1.20
  }
};
```

---

# PART 5. API 엔드포인트 설계 (참고용)

원본 프로젝트의 API 스타일에 맞추되, 아래 엔드포인트가 반드시 필요하다.

```
--- 인증 ---
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me

--- 유저/프로필 ---
GET    /api/users/:id
PUT    /api/users/:id/profile     (식물 선택, 닉네임 변경 등)
GET    /api/users/:id/posts       (내 글 목록)
GET    /api/users/:id/drops       (물방울 이력)

--- 게시글 ---
GET    /api/posts                 (피드 - query param으로 category 필터)
POST   /api/posts                 (글 작성 → 물방울 자동 지급 트리거)
GET    /api/posts/:id
PUT    /api/posts/:id
DELETE /api/posts/:id

--- 댓글 ---
GET    /api/posts/:id/comments
POST   /api/posts/:id/comments
DELETE /api/comments/:id

--- 리더보드 ---
GET    /api/leaderboard            (query: period=monthly|all_time)
GET    /api/leaderboard/party/:id  (파티 내 개인 순위)

--- 파티 ---
GET    /api/parties
GET    /api/parties/:id

--- 퀘스트 ---
GET    /api/quests                 (활성 퀘스트 목록)
GET    /api/quests/:id
POST   /api/quests/:id/complete    (퀘스트 인증 = 글 작성과 연동)

--- 투표 ---
POST   /api/quests/:id/vote
GET    /api/quests/:id/votes       (투표 결과)

--- 알림 ---
GET    /api/notifications
PUT    /api/notifications/:id/read
PUT    /api/notifications/read-all

--- 관리자 (GM) ---
POST   /api/admin/quests           (퀘스트 생성)
PUT    /api/admin/quests/:id       (퀘스트 수정)
DELETE /api/admin/quests/:id       (퀘스트 삭제)
POST   /api/admin/drops/award      (물방울 수동 지급: { userId, amount, reason })
POST   /api/admin/drops/deduct     (물방울 수동 차감: { userId, amount, reason })
PUT    /api/admin/users/:id        (유저 정보 수정: 파티, 식물, 직업군 등)
POST   /api/admin/parties          (파티 생성)
PUT    /api/admin/parties/:id      (파티 수정)
GET    /api/admin/stats            (통계 대시보드 데이터)
POST   /api/admin/announcements    (공지 작성)
```

---

# PART 6. 초기 시드 데이터

앱 최초 배포 시 다음 데이터를 시드로 삽입해야 한다.

```javascript
// 파티 5개 (GM이 나중에 파티원을 배정)
const parties = [
  { name: '파티 이름은 4월 네이밍 퀘스트에서 결정' },
  // 초기에는 TG1 ~ TG5 등 임시 이름 사용
  { name: 'TG1' },
  { name: 'TG2' },
  { name: 'TG3' },
  { name: 'TG4' },
  { name: 'TG5' }
];

// GM 계정 1개
const adminUser = {
  email: 'ca@company.com', // CA(조직문화 담당) 계정
  is_admin: true,
  nickname: '게임 마스터'
};
```

---

# PART 7. 핵심 주의사항 요약

```
1. 물방울 자동 지급은 반드시 서버 사이드에서 처리한다. 클라이언트에서 물방울 수치를 조작할 수 없어야 한다.

2. 중복 제한(일일 1회, 주간 1회)은 DB의 UNIQUE KEY로 보장한다. 애플리케이션 레벨 체크만으로는 동시성 이슈가 발생할 수 있다.

3. 모든 물방울 변동은 drop_transactions 테이블에 기록한다. 이 테이블이 유일한 진실의 원천(Single Source of Truth)이며, users.total_drops는 이 테이블의 합계와 항상 일치해야 한다.

4. exp_multiplier(식물 난이도 가중치)는 물방울 지급 시 자동 적용된다. 결과는 반올림하여 정수로 지급한다.
   예: 동료 칭찬 30 × 1.2(어려움) = 36 물방울

5. 리더보드는 파티 합산이 메인이다. 개인 간 과도한 경쟁을 방지하기 위함이다.

6. 앱은 모바일 웹 환경에 최적화한다. 20명 모두 스마트폰으로 접속할 것이다. 반응형 디자인 필수.

7. 고양이 관련 문구/이미지/아이콘이 하나라도 남아있으면 안 된다. 전수 검사한다.

8. 원본 프로젝트의 기술 스택을 최대한 유지한다. 불필요한 기술 스택 변경은 하지 않는다.
```

---

> 이 문서는 Claude Code에게 전달하는 작업 지시서이다.
> Phase 0부터 순서대로 진행하며, 각 Phase 완료 시 결과를 확인받은 후 다음 Phase로 넘어간다.
