SPEC 6-G: 대화(쪽지함) 기능 — WebSocket(STOMP)

## Context
닉네임 클릭 시 1:1 대화를 시작하고, 채팅 UI로 쪽지를 주고받는 기능. WebSocket(STOMP)으로 실시간 메시지 전송, DB에 영구 저장. 나가기 시 "나갔습니다" 표시, 양쪽 다 나가면
대화방+메시지 삭제.

## 파일 목록

### 신규 생성 (14개)

**백엔드 (10개)**
1. `backend/.../entity/Conversation.java` — 대화방 엔티티
2. `backend/.../entity/Message.java` — 메시지 엔티티
3. `backend/.../repository/ConversationRepository.java`
4. `backend/.../repository/MessageRepository.java`
5. `backend/.../dto/MessageRequest.java`
6. `backend/.../dto/ConversationResponse.java`
7. `backend/.../dto/MessageResponse.java`
8. `backend/.../service/ConversationService.java`
9. `backend/.../controller/ConversationController.java` — REST API
10. `backend/.../config/WebSocketConfig.java` — STOMP 설정 + JWT 인증 인터셉터

**프론트엔드 (4개)**
11. `frontend/src/lib/websocket.ts` — STOMP 클라이언트 헬퍼
12. `frontend/src/app/conversations/page.tsx` — 대화 목록
13. `frontend/src/app/conversations/[id]/page.tsx` — 채팅방 라우트
14. `frontend/src/components/ChatRoom.tsx` — 채팅 UI

### 수정 (8개)
1. `backend/build.gradle` — websocket 의존성 추가
2. `backend/.../repository/UserRepository.java` — `findByNickname` 추가
3. `backend/.../config/SecurityConfig.java` — `/api/conversations/**`, `/ws/**` 규칙
4. `frontend/src/types/index.ts` — ConversationInfo, ChatMessage 타입
5. `frontend/src/lib/api.ts` — 대화 API 함수 4개
6. `frontend/src/components/Header.tsx` — 쪽지함 버튼 (닉네임과 새글쓰기 사이)
7. `frontend/src/components/PostDetail.tsx` — 작성자 닉네임 클릭 → 대화
8. `frontend/src/components/CommentSection.tsx` — 댓글 닉네임 클릭 → 대화

---

## Phase 1: 백엔드 의존성
`build.gradle`에 `implementation 'org.springframework.boot:spring-boot-starter-websocket'` 추가

## Phase 2: 엔티티 + Repository

### Conversation.java
- `user1`, `user2` (@ManyToOne User) — 항상 user1.id < user2.id 정규화 → UniqueConstraint
- `user1Left`, `user2Left` (boolean)
- `updatedAt` — 메시지마다 갱신 (목록 정렬)
- 인덱스: `user1_id`, `user2_id`, `updatedAt DESC`

### Message.java
- `conversation` (@ManyToOne), `sender` (@ManyToOne User), `content` (VARCHAR 2000), `systemMessage` (boolean)
- 인덱스: `(conversation_id, createdAt ASC)`

### ConversationRepository
- `findByUser1AndUser2(User, User)` — 기존 대화 조회
- `findActiveByUser(User)` — 내 활성 대화 목록 (JPQL)

### MessageRepository
- `findByConversationOrderByCreatedAtAsc(Conversation)`
- `deleteByConversation(Conversation)`

### UserRepository (수정)
- `findByNickname(String)` 1줄 추가

## Phase 3: DTO + Service + Controller

### ConversationService 핵심 로직
- **startConversation**: id 정규화 → 기존 대화 재사용 or 신규 생성 (나갔던 유저면 left=false 복귀)
- **sendMessage**: 참여자 검증 + 상대 나간 상태면 전송 차단 + 저장 + updatedAt 갱신
- **leaveConversation**: left=true + 시스템 메시지 + 양쪽 다 left면 전체 삭제

### ConversationController — REST
- `POST /api/conversations` — 대화 시작 `{ "nickname": "상대" }`
- `GET /api/conversations` — 내 대화 목록
- `GET /api/conversations/{id}/messages` — 메시지 조회
- `POST /api/conversations/{id}/messages` — 메시지 전송 (REST fallback)
- `POST /api/conversations/{id}/leave` — 나가기

## Phase 4: WebSocket

### WebSocketConfig.java
- SimpleBroker: `/topic`, AppPrefix: `/app`, 엔드포인트: `/ws` (SockJS)
- STOMP CONNECT 시 JWT 인증 (ChannelInterceptor — `JwtUtil.validateToken` + `extractEmail`)

### 메시지 핸들러
- `@MessageMapping("/chat/{conversationId}")` → Service.sendMessage → `/topic/messages/{conversationId}` broadcast

### SecurityConfig
- `.requestMatchers("/api/conversations/**").authenticated()`
- `.requestMatchers("/ws/**").permitAll()` (STOMP 레벨에서 JWT 인증)

## Phase 5: 프론트엔드

### 패키지: `@stomp/stompjs`, `sockjs-client`

### websocket.ts — STOMP Client 싱글턴, JWT 포함, reconnectDelay 5초

### conversations/page.tsx — 대화 목록
- getConversations() 호출, 상대 닉네임/마지막 메시지/시간/otherLeft 표시

### ChatRoom.tsx — 채팅 UI
- 상단: 상대 닉네임 + 뒤로가기 + 나가기
- 중간: 메시지 영역 (본인=오른쪽 주황, 상대=왼쪽 회색, 시스템=중앙)
- 하단: 고정 입력폼
- STOMP `/topic/messages/{id}` 구독, `/app/chat/{id}`로 publish
- 자동 스크롤 (messagesEndRef)

### Header.tsx — 닉네임과 새글쓰기 사이에 "쪽지함" Link

### PostDetail.tsx, CommentSection.tsx — 닉네임 클릭 → startConversation → /conversations/{id}

## 검증
1. 닉네임 클릭 → 대화방 생성 → 실시간 메시지 송수신
2. 같은 상대 재대화 → 기존 대화방 재사용
3. 나가기 → "나갔습니다" 시스템 메시지
4. 양쪽 나가기 → DB 삭제
5. 서버 재시작 후 데이터 유지
6. 나간 대화방 재진입 시 left=false 복귀
