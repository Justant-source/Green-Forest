---
title: L3 — 엔티티 · enum · 관계
last_updated: 2026-08-31
---

# backend/30-components/entity-and-enum

> `@Entity` 클래스 **43개** (2026-08-31 `find`+`grep`). 계획서 47은 코드와 불일치.

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

---

## §2. 주요 관계 (요약)

- `Post` ← `Comment`, `PostLike`, `PostImage`, `Vote`, `Bookmark`
- `User` ← `Notification`, `PlantGrowth`, `AttendanceCheckin`
- `Event` ← 참여·빙고·사진전 엔티티
- `Survey` ← `SurveyOption` ← `SurveyVote`

---

## §3. 도메인 enum (엔티티 패키지)

`ThumbnailService.Size` 같은 서비스 내부 enum은 제외. 파일 기준 19개.

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
