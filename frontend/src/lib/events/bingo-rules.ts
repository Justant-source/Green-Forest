import type { PhotoBingoRewards } from "./types";

/** 3x3 빙고판의 모든 라인 (가로 3 + 세로 3 + 대각선 2 = 8줄) */
export const BINGO_LINES: ReadonlyArray<ReadonlyArray<number>> = [
  [0, 1, 2],
  [3, 4, 5],
  [6, 7, 8],
  [0, 3, 6],
  [1, 4, 7],
  [2, 5, 8],
  [0, 4, 8],
  [2, 4, 6],
];

/** 각 라인의 사람이 읽기 쉬운 이름. BINGO_LINES 와 같은 순서. */
export const BINGO_LINE_NAMES: readonly string[] = [
  "가로 1행",
  "가로 2행",
  "가로 3행",
  "세로 1열",
  "세로 2열",
  "세로 3열",
  "대각선 ↘",
  "대각선 ↙",
];

export function completedLineNames(indices: Set<number>): string[] {
  const names: string[] = [];
  BINGO_LINES.forEach((line, i) => {
    if (line.every((idx) => indices.has(idx))) names.push(BINGO_LINE_NAMES[i]);
  });
  return names;
}

export function countCompletedLines(indices: Set<number>): number {
  let n = 0;
  for (const line of BINGO_LINES) {
    if (line.every((i) => indices.has(i))) n++;
  }
  return n;
}

export function calculateReward(approved: Set<number>, rewards: PhotoBingoRewards): number {
  if (approved.size === 9) return rewards.blackout;
  const lines = countCompletedLines(approved);
  if (lines >= 5) return rewards.line5;
  if (lines >= 3) return rewards.line3;
  return 0;
}

/** 업로드(APPROVED 포함) 상태를 "예상 보상" 기준으로 계산. */
export function estimateReward(uploadedOrApproved: Set<number>, rewards: PhotoBingoRewards): {
  reward: number;
  lines: number;
  tier: "none" | "line3" | "line5" | "blackout";
} {
  const lines = countCompletedLines(uploadedOrApproved);
  if (uploadedOrApproved.size === 9) {
    return { reward: rewards.blackout, lines, tier: "blackout" };
  }
  if (lines >= 5) return { reward: rewards.line5, lines, tier: "line5" };
  if (lines >= 3) return { reward: rewards.line3, lines, tier: "line3" };
  return { reward: 0, lines, tier: "none" };
}
