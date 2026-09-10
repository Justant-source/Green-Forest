---
title: L3 — 엔티티 · enum · 관계
last_updated: 2026-09-10
---

# backend/30-components/entity-and-enum

> `@Entity` 클래스 개수는 **코드가 정본**. 요청 도메인 4개(`SnackCatalogItem`, `SnackRequest`, `SupplyRequest`, `CatalogSyncRun`)가 추가됐다.

상세 테이블명·마이그레이션은 [../40-data.md](../40-data.md).

---

## §1. 도메인별 엔티티

| 도메인 | 엔티티 |
|---|---|
| User | `User`, `Party`, `PlantGrowth`, `GrowthScoreLog`, `BirthdayAcknowledgement` |
| Post | `Post`, `PostImage`, `PostLike`, `PostTag`, `Comment`, `Vote`, `Bookmark`, `Category`, `CategoryRequest` |
| Survey | `Survey`, `SurveyOption`, `SurveyVote`, `SurveyDelivery` |
| Quest | `Quest`, `QuestCompletion`, `QuestCompletionLog` |
| Attendance / Gacha | `AttendanceCheckin`, `AttendancePhrase`, `GachaPrize`, `GachaDraw`, `GachaPityStack` |
| Event | `Event`, `EventParticipation`, `PhotoBingoCell`, `PhotoBingoSubmission`, `PhotoExhibitionConfig`, `PhotoExhibitionSubmission`, `PhotoExhibitionImage`, `PhotoExhibitionVote`, `PhotoExhibitionRewardGrant` |
| Chat | `Conversation`, `Message` |
| System | `SystemSetting`, `Notification`, `Announcement`, `WeeklyReport`, `OutboundEvent`, `DropTransaction` |
| Request | `SnackCatalogItem`, `SnackRequest`, `SupplyRequest`, `CatalogSyncRun` |

---

## §2. 주요 관계 (요약)

- `Post` ← `Comment`, `PostLike`, `PostImage`, `Vote`, `Bookmark`
- `User` ← `Notification`, `PlantGrowth`, `AttendanceCheckin`, `SnackRequest`, `SupplyRequest`
- `SnackCatalogItem` ← `SnackRequest`
- `Event` ← 참여·빙고·사진전 엔티티
- `Survey` ← `SurveyOption` ← `SurveyVote`

`SurveyOption.description`은 항목 설명(최대 300자, nullable). `text_content`는 항목 제목.

---

## §3. 도메인 enum (엔티티 패키지)

`ThumbnailService.Size` 같은 서비스 내부 enum은 제외. 파일 기준 기존 19개 + 요청 도메인 4개.

| enum | 파일 |
|---|---|
| `PostStatus` | `backend/src/main/java/com/vgc/entity/PostStatus.java` |
| `JobClass` | `backend/src/main/java/com/vgc/entity/JobClass.java` |
| `PlantType` | `backend/src/main/java/com/vgc/entity/PlantType.java` |
| `Element` | `backend/src/main/java/com/vgc/entity/Element.java` |
| `Difficulty` | `backend/src/main/java/com/vgc/entity/Difficulty.java` |
| `NotificationType` | `backend/src/main/java/com/vgc/entity/NotificationType.java` |
| `AnnouncementType` | `backend/src/main/java/com/vgc/entity/AnnouncementType.java` |
| `DropReasonType` | `backend/src/main/java/com/vgc/entity/DropReasonType.java` |
| `GrowthScoreReason` | `backend/src/main/java/com/vgc/entity/GrowthScoreReason.java` |
| `AttendanceDeliveryStatus` | `backend/src/main/java/com/vgc/entity/AttendanceDeliveryStatus.java` |
| `AttendanceMessageType` | `backend/src/main/java/com/vgc/entity/AttendanceMessageType.java` |
| `GachaPrizeTier` | `backend/src/main/java/com/vgc/entity/GachaPrizeTier.java` |
| `GachaDeliveryStatus` | `backend/src/main/java/com/vgc/entity/GachaDeliveryStatus.java` |
| `SurveyDeliveryStatus` | `backend/src/main/java/com/vgc/entity/SurveyDeliveryStatus.java` |
| `SurveyOptionType` | `backend/src/main/java/com/vgc/entity/SurveyOptionType.java` |
| `EventStatus` | `backend/src/main/java/com/vgc/entity/event/EventStatus.java` |
| `EventType` | `backend/src/main/java/com/vgc/entity/event/EventType.java` |
| `CellScoreStatus` | `backend/src/main/java/com/vgc/entity/event/photobingo/CellScoreStatus.java` |
| `PhotoExhibitionPhase` | `backend/src/main/java/com/vgc/service/event/photoexhibition/PhotoExhibitionPhase.java` |
| `RequestStatus` | PENDING, PURCHASED, REJECTED |
| `RequestRejectReason` | OUT_OF_STOCK, POLICY, DUPLICATE, OTHER |
| `CatalogSyncStatus` | RUNNING, SUCCESS, FAIL |
| `RequestKind` | SNACK, SUPPLY (알림 딥링크) |
| `SnackSsgCategory` | 과자/간식 하위: SNACK, COOKIE, SAND, PIE, BABY, TRADITIONAL, ANJU, CANDY, JELLY, CHOCOLATE, CEREAL, ICE_CREAM |

`NotificationType`에 `REQUEST("요청")` 상수가 있다. `Notification.relatedRequestKind`는 `RequestKind`.
