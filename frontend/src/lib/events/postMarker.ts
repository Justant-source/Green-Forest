import type { CellScoreStatus, EventStatus } from "./types";

/**
 * 백엔드 `PhotoBingoPostSyncService`가 Post.content에 삽입하는 마커 스펙과 정확히 일치해야 한다.
 * 형식: `<!--photo-bingo:{json}-->`
 */
export interface PhotoBingoMarkerPayload {
  submissionId: number;
  eventId: number;
  eventTitle: string;
  eventStatus: EventStatus;
  achievedLines: number;
  finalRewardDrops: number;
  cells: Array<{
    idx: number;
    theme: string;
    imageUrl: string | null;
    scoreStatus: CellScoreStatus;
  }>;
}

const MARKER_REGEX = /<!--photo-bingo:([\s\S]*?)-->/;

export interface ParsedPost {
  cleanContent: string;
  bingo: PhotoBingoMarkerPayload | null;
}

export function parsePhotoBingoMarker(content: string | null | undefined): ParsedPost {
  if (!content) return { cleanContent: "", bingo: null };
  const match = content.match(MARKER_REGEX);
  if (!match) return { cleanContent: content, bingo: null };
  const json = match[1];
  try {
    const payload = JSON.parse(json) as PhotoBingoMarkerPayload;
    const cleanContent = content.replace(MARKER_REGEX, "").trim();
    return { cleanContent, bingo: payload };
  } catch {
    return { cleanContent: content, bingo: null };
  }
}
