export type EventStatus = "DRAFT" | "SCHEDULED" | "ACTIVE" | "ENDED" | "SCORED";
export type EventType = "PHOTO_BINGO" | "PHOTO_EXHIBITION";
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
export interface PhotoExhibitionConfig { submissionStart: string; submissionEnd: string; reviewEnd: string; votingEnd: string; }
export interface PhotoExhibitionImage { id: number; imageUrl: string; sortOrder: number; representative: boolean; }
export interface PhotoExhibitionSubmission { id: number; title: string; introduction: string; images: PhotoExhibitionImage[]; mine: boolean; authorNickname?: string | null; finalVotes?: number | null; resultTier?: string | null; }
export interface PhotoExhibitionAdminSubmission extends PhotoExhibitionSubmission { excluded: boolean; exclusionReason?: string | null; voteCount: number; }
export interface PhotoExhibitionPreview { validParticipantCount:number; uniqueVoterCount:number; selectionCount:number; participantRewardTotal:number; voterRewardTotal:number; rankRewardTotal:number; grandTotal:number; candidates:{submissionId:number;authorNickname:string;title:string;voteCount:number;proposedTier?:string|null;reward:number}[]; }
export interface PhotoExhibitionVoterAudit { submissionId:number; voterNickname:string; workTitle:string; }

export interface Event {
  id: number;
  type: EventType;
  title: string;
  description?: string | null;
  startAt: string; // ISO LocalDateTime
  endAt: string;
  status: EventStatus;
  config: PhotoBingoConfig | null;
  photoExhibitionConfig?: PhotoExhibitionConfig | null;
  phase?: "SCHEDULED" | "SUBMISSION" | "REVIEW" | "VOTING" | "TALLY_PENDING" | "RESULT";
  serverNow?: string;
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
  config?: PhotoBingoConfig | null;
  photoExhibitionConfig?: PhotoExhibitionConfig;
}

export interface PhotoBingoActivity {
  userId: number;
  userNickname: string;
  cellIndex: number;
  theme: string;
  uploadedAt: string;
  uploadedCount: number;
}
