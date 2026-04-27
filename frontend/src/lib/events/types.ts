export type EventStatus = "DRAFT" | "SCHEDULED" | "ACTIVE" | "ENDED" | "SCORED";
export type EventType = "PHOTO_BINGO";
export type CellScoreStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface PhotoBingoRewards {
  line3: number;
  line5: number;
  blackout: number;
}

export interface PhotoBingoConfig {
  themes: string[];
  rewards: PhotoBingoRewards;
}

export interface Event {
  id: number;
  type: EventType;
  title: string;
  description?: string | null;
  startAt: string; // ISO LocalDateTime
  endAt: string;
  status: EventStatus;
  config: PhotoBingoConfig | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface EventModeResponse {
  mode: "ACTIVE" | "NONE";
  event: Event | null;
  serverNow: string;
}

export interface PhotoBingoCellDto {
  id: number;
  cellIndex: number;
  theme: string;
  imageUrl: string | null;
  uploadedAt: string | null;
  scoreStatus: CellScoreStatus;
  scoreComment: string | null;
}

export interface PhotoBingoSubmissionDto {
  submissionId: number;
  userId: number;
  userNickname: string;
  caption: string | null;
  cells: PhotoBingoCellDto[];
  achievedLines: number;
  finalRewardDrops: number;
}

export interface CreateEventRequest {
  type: EventType;
  title: string;
  description?: string;
  startAt: string;
  endAt: string;
  config: PhotoBingoConfig;
}

export interface PhotoBingoActivity {
  userId: number;
  userNickname: string;
  cellIndex: number;
  theme: string;
  uploadedAt: string;
  uploadedCount: number;
}
