# 🐛 그린 포레스트 — 버그 및 개선 사항 (v1.0 테스트 결과)

---

## 1. 직업(식물) 변경 잠금 기능 없음

**현상:** 유저가 마이가든에서 식물 종류를 자유롭게 변경할 수 있음.

**기대 동작:**
- 최초 1회 선택 후 **변경 불가** (잠금)
- 관리자가 해당 유저에게 **변경 권한을 부여**한 경우에만 재선택 가능

**수정 방향:**
- `users` 테이블에 `plant_locked BOOLEAN DEFAULT FALSE` 추가 (또는 최초 선택 여부로 판단)
- `PUT /api/users/me/profile` — `plantType` 변경 시 이미 설정된 경우 차단 (관리자 권한 플래그 없으면 거부)
- `PUT /api/admin/users/{id}` — 관리자가 `plantType`을 `null`로 초기화하면 유저가 재선택 가능하도록

---

## 2. 마이가든 직업군 라벨이 영문 표시 + 레이아웃 깨짐

**현상:** 프로필 카드에 직업군이 `Guardian`, `Healer` 등 영문으로 표시되며 긴 텍스트가 카드 밖으로 삐져나옴.

**수정:**
- **프론트엔드** `frontend/src/app/garden/page.tsx` — 프로필 카드 영역에서 `jobClassLabelEn` 대신 `jobClassLabel` (한글: 탱커, 힐러, 버퍼, 딜러) 사용
- 물방울/파티/직업군 3열 그리드의 텍스트에 `truncate` 또는 `text-sm` 적용하여 오버플로 방지

```tsx
// 변경 전
<div className="text-2xl font-bold text-gray-700">{user.jobClassLabelEn || "-"}</div>

// 변경 후
<div className="text-2xl font-bold text-gray-700 truncate">{user.jobClassLabel || "-"}</div>
```

---

## 3. 동료칭찬 태깅 시 태깅 보너스(5 물방울) 미지급

**현상:** 동료칭찬 글 작성 시 `taggedNicknames`를 전송해도 태깅된 유저에게 물방울 5가 지급되지 않음.

**원인 추정:** `PostService.createPost()`에서 태깅 닉네임을 조회한 뒤 `DropService.awardDropsForPost()`에 전달하는 흐름에서 실제 `taggedUsers` 리스트가 비어있을 가능성.

**디버깅 포인트:**
1. `PostController.createPost()` — `taggedNicknamesStr` 파라미터가 정상 파싱되는지 확인
2. `PostService.createPost()` — `userRepository.findByNicknameIn(taggedNicknames)` 결과가 빈 리스트인지 확인
3. `DropService.awardTagBonuses()` — 실제 호출 여부 및 `postTagRepository.save()` 실행 여부 확인

**추가 확인:**
- 자기 자신 태깅 → 지급 차단 (코드상 `taggedUser.getId().equals(author.getId())` 체크 존재 — 정상 여부 확인)
- 같은 글에서 동일인 중복 태깅 → 1회만 지급 (`UNIQUE KEY` 및 `existsByPostIdAndTaggedUserId` 체크 존재 — 정상 여부 확인)

---

## 4. 하단 탭 네비게이션이 PC(웹)에서 보이지 않음

**현상:** 하단 탭이 `md:hidden` 클래스로 인해 데스크톱 화면에서 숨겨짐.

**수정:** `frontend/src/components/BottomNav.tsx`

```tsx
// 변경 전
<nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50 md:hidden safe-area-bottom">

// 변경 후 (md:hidden 제거)
<nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50 safe-area-bottom">
```

> 제거 후 데스크톱에서도 하단 탭이 보이게 되므로, `body`의 `pb-16 md:pb-0` → `pb-16`으로 통일 필요 (`layout.tsx`).

---

## 5. /garden 직접 접근 시 로그인 상태인데 /login으로 리다이렉트

**현상:** `http://localhost:3000/garden`을 주소창에 직접 입력하거나 새로고침하면, 로그인 상태임에도 `/login`으로 리다이렉트됨.

**원인:** `garden/page.tsx`의 `useEffect`에서 `isLoggedIn`을 체크하지만, `AuthContext`의 `authLoaded`가 `false`인 초기 렌더 시점에 `isLoggedIn === false`로 판단되어 리다이렉트 발생.

**수정:** `frontend/src/app/garden/page.tsx`

```tsx
// 변경 전
useEffect(() => {
  if (!isLoggedIn) {
    router.replace("/login");
    return;
  }
  // ...
}, [isLoggedIn, router]);

// 변경 후 (authLoaded 체크 추가)
const { isLoggedIn, handleLogout, authLoaded } = useAuth();

useEffect(() => {
  if (!authLoaded) return;          // 아직 로딩 중이면 대기
  if (!isLoggedIn) {
    router.replace("/login");
    return;
  }
  // ...
}, [authLoaded, isLoggedIn, router]);
```

> 같은 패턴이 적용된 다른 페이지(`/quests`, `/notifications`, `/conversations`, `/posts/new`, `/posts/[id]/edit`)도 동일하게 수정 필요.

---

## 6. CORS allowedMethods 명시적 지정 권장

**현상:** `SecurityConfig.java`에서 `config.setAllowedMethods(List.of("*"))`로 설정되어 있음. 일부 브라우저/프록시 환경에서 preflight 실패 가능.

**수정:** `backend/src/main/java/com/vgc/config/SecurityConfig.java`

```java
// 변경 전
config.setAllowedMethods(List.of("*"));

// 변경 후
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
```

---

## 7. 중복 회원가입 시 에러 메시지 body 비어있음

**현상:** 이미 존재하는 이메일/닉네임으로 `POST /api/auth/register` 호출 시 HTTP 500 반환, body에 메시지 없음.

**원인:** `AuthController.register()`에서 `AuthService`가 `RuntimeException`을 throw하지만, 글로벌 예외 핸들러가 없어 Spring 기본 에러 응답(또는 빈 body)이 반환됨.

**수정:** `backend/src/main/java/com/vgc/controller/AuthController.java`

```java
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody AuthRequest request) {
    try {
        AuthResponse response = authService.register(
                request.getEmail(), request.getPassword(), request.getNickname());
        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }
}
```

---

## 수정 우선순위

| 순위 | 항목 | 심각도 |
|------|------|--------|
| 1 | #3 태깅 보너스 미지급 | 🔴 Critical (핵심 정책 오류) |
| 2 | #5 /garden 리다이렉트 | 🔴 Critical (사용 불가) |
| 3 | #7 회원가입 에러 메시지 | 🟡 High |
| 4 | #1 직업 변경 잠금 | 🟡 High (정책 미구현) |
| 5 | #4 하단 탭 PC 미표시 | 🟡 High (UX) |
| 6 | #2 직업군 영문 표시 | 🟢 Medium (UI) |
| 7 | #6 CORS 명시적 지정 | 🟢 Low (예방적) |
