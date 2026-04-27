"use client";

import { useEffect, useMemo, useState } from "react";
import { adminListSubmissions } from "@/lib/events/api";
import { completedLineNames } from "@/lib/events/bingo-rules";
import type { PhotoBingoRewards, PhotoBingoSubmissionDto, Event } from "@/lib/events/types";

interface Props {
  event: Event;
  onClose: () => void;
}

type Tier = "blackout" | "line5" | "line3" | "none";

interface SubmissionBreakdown {
  submission: PhotoBingoSubmissionDto;
  uploaded: number;
  approved: number;
  rejected: number;
  pending: number;
  notUploaded: number;
  approvedIndices: number[];
  completedLines: string[];
  tier: Tier;
  tierLabel: string;
  ruleDescription: string;
  expectedDrops: number;
}

function computeTier(approvedCount: number, lineCount: number): Tier {
  if (approvedCount === 9) return "blackout";
  if (lineCount >= 5) return "line5";
  if (lineCount >= 3) return "line3";
  return "none";
}

function tierMeta(tier: Tier, rewards: PhotoBingoRewards): { label: string; cls: string; rule: string; drops: number } {
  switch (tier) {
    case "blackout":
      return {
        label: "블랙아웃",
        cls: "bg-purple-100 text-purple-800",
        rule: `승인 9칸 전부 → 블랙아웃 보상`,
        drops: rewards.blackout,
      };
    case "line5":
      return {
        label: "5줄 이상",
        cls: "bg-indigo-100 text-indigo-800",
        rule: `5줄 이상 달성 → line5 보상`,
        drops: rewards.line5,
      };
    case "line3":
      return {
        label: "3줄",
        cls: "bg-forest-100 text-forest-700",
        rule: `3~4줄 달성 → line3 보상`,
        drops: rewards.line3,
      };
    default:
      return {
        label: "미달성",
        cls: "bg-gray-100 text-gray-600",
        rule: "3줄 미달성 → 지급 없음",
        drops: 0,
      };
  }
}

function buildBreakdown(s: PhotoBingoSubmissionDto, rewards: PhotoBingoRewards): SubmissionBreakdown {
  const uploaded = s.cells.filter((c) => c.imageUrl).length;
  const approved = s.cells.filter((c) => c.scoreStatus === "APPROVED").length;
  const rejected = s.cells.filter((c) => c.scoreStatus === "REJECTED").length;
  const pending = s.cells.filter((c) => c.imageUrl && c.scoreStatus === "PENDING").length;
  const notUploaded = 9 - uploaded;
  const approvedIndices = s.cells
    .filter((c) => c.scoreStatus === "APPROVED")
    .map((c) => c.cellIndex)
    .sort((a, b) => a - b);
  const completedLines = completedLineNames(new Set(approvedIndices));
  const tier = computeTier(approved, completedLines.length);
  const meta = tierMeta(tier, rewards);
  return {
    submission: s,
    uploaded,
    approved,
    rejected,
    pending,
    notUploaded,
    approvedIndices,
    completedLines,
    tier,
    tierLabel: meta.label,
    ruleDescription: meta.rule,
    expectedDrops: meta.drops,
  };
}

export default function BingoRewardsSummary({ event, onClose }: Props) {
  const [submissions, setSubmissions] = useState<PhotoBingoSubmissionDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const rewards = event.config?.rewards ?? { line3: 0, line5: 0, blackout: 0 };

  useEffect(() => {
    adminListSubmissions(event.id)
      .then(setSubmissions)
      .catch((e) => setError(e?.message ?? "로딩 실패"));
  }, [event.id]);

  const breakdowns = useMemo(() => {
    if (!submissions) return null;
    return [...submissions]
      .map((s) => buildBreakdown(s, rewards))
      .sort((a, b) => {
        if (b.submission.finalRewardDrops !== a.submission.finalRewardDrops) {
          return b.submission.finalRewardDrops - a.submission.finalRewardDrops;
        }
        if (b.submission.achievedLines !== a.submission.achievedLines) {
          return b.submission.achievedLines - a.submission.achievedLines;
        }
        return a.submission.userNickname.localeCompare(b.submission.userNickname);
      });
  }, [submissions, rewards]);

  const totals = useMemo(() => {
    if (!submissions) return { participants: 0, rewarded: 0, totalDrops: 0 };
    return submissions.reduce(
      (acc, s) => {
        acc.participants += 1;
        if (s.finalRewardDrops > 0) acc.rewarded += 1;
        acc.totalDrops += s.finalRewardDrops;
        return acc;
      },
      { participants: 0, rewarded: 0, totalDrops: 0 }
    );
  }, [submissions]);

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-end sm:items-center justify-center p-0 sm:p-4" onClick={onClose}>
      <div
        className="bg-white w-full sm:max-w-2xl sm:rounded-2xl rounded-t-2xl max-h-[92vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <div>
            <div className="text-xs text-gray-500">지급 결과</div>
            <div className="font-semibold">{event.title}</div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="m-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
            {error}
          </div>
        )}

        {!submissions ? (
          <div className="flex justify-center py-10">
            <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : (
          <>
            <div className="px-4 py-3 border-b bg-forest-50/50">
              <div className="grid grid-cols-3 gap-2 text-center text-xs mb-2">
                <div>
                  <div className="text-gray-500">참여자</div>
                  <div className="font-semibold text-gray-900 text-base">{totals.participants}명</div>
                </div>
                <div>
                  <div className="text-gray-500">수령자</div>
                  <div className="font-semibold text-forest-700 text-base">{totals.rewarded}명</div>
                </div>
                <div>
                  <div className="text-gray-500">총 지급</div>
                  <div className="font-semibold text-forest-700 text-base">💧{totals.totalDrops}</div>
                </div>
              </div>
              <div className="text-[11px] text-gray-600 leading-relaxed border-t pt-2 mt-1">
                <div className="font-semibold text-gray-700 mb-0.5">보상 규칙</div>
                <div>• 3줄 달성 → 💧{rewards.line3}</div>
                <div>• 5줄 이상 → 💧{rewards.line5}</div>
                <div>• 블랙아웃(9칸 전부 승인) → 💧{rewards.blackout}</div>
                <div className="text-gray-400">* 최고 등급 하나만 지급됩니다.</div>
              </div>
            </div>

            <div className="overflow-y-auto flex-1">
              {breakdowns && breakdowns.length === 0 && (
                <div className="text-center text-gray-500 py-10 text-sm">참여자가 없습니다.</div>
              )}
              <ul className="divide-y">
                {breakdowns?.map((b, i) => {
                  const rank = i + 1;
                  const rankCls =
                    b.submission.finalRewardDrops > 0 && rank === 1 ? "bg-yellow-400 text-yellow-900" :
                    b.submission.finalRewardDrops > 0 && rank === 2 ? "bg-gray-300 text-gray-700" :
                    b.submission.finalRewardDrops > 0 && rank === 3 ? "bg-amber-600/80 text-white" :
                    "bg-gray-100 text-gray-500";
                  const tierCls =
                    b.tier === "blackout" ? "bg-purple-100 text-purple-800" :
                    b.tier === "line5" ? "bg-indigo-100 text-indigo-800" :
                    b.tier === "line3" ? "bg-forest-100 text-forest-700" :
                    "bg-gray-100 text-gray-600";
                  const paid = b.submission.finalRewardDrops;
                  const mismatch = paid !== b.expectedDrops;
                  return (
                    <li key={b.submission.submissionId} className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${rankCls}`}>
                          {rank}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-gray-900 truncate">
                              {b.submission.userNickname}
                            </span>
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${tierCls}`}>
                              {b.tierLabel}
                            </span>
                          </div>
                        </div>
                        <div className={`text-right font-semibold ${paid > 0 ? "text-forest-700" : "text-gray-400"}`}>
                          {paid > 0 ? `+💧${paid}` : "—"}
                        </div>
                      </div>

                      <div className="mt-2 ml-10 space-y-1 text-xs text-gray-600">
                        <div className="flex flex-wrap gap-x-3 gap-y-0.5">
                          <span>✅ 승인 <b className="text-gray-800">{b.approved}</b>칸</span>
                          <span>❌ 반려 <b className="text-gray-800">{b.rejected}</b>칸</span>
                          {b.pending > 0 && (
                            <span>⏳ 미채점 <b className="text-orange-600">{b.pending}</b>칸</span>
                          )}
                          <span>⬜ 미업로드 <b className="text-gray-800">{b.notUploaded}</b>칸</span>
                        </div>

                        <div>
                          {b.completedLines.length > 0 ? (
                            <>
                              완성 라인 <b className="text-gray-800">{b.submission.achievedLines}줄</b>
                              {": "}
                              <span className="text-gray-700">{b.completedLines.join(", ")}</span>
                            </>
                          ) : (
                            <>완성 라인 <b className="text-gray-800">0줄</b></>
                          )}
                        </div>

                        <div className="text-gray-500">
                          적용 규칙: <span className="text-gray-700">{b.ruleDescription}</span>
                          {b.expectedDrops > 0 && (
                            <> → 💧<b className="text-gray-800">{b.expectedDrops}</b></>
                          )}
                        </div>

                        {mismatch && (
                          <div className="text-orange-600">
                            ⚠ 예상 지급액(💧{b.expectedDrops})과 실제 지급액(💧{paid})이 다릅니다.
                            Finalize 이후 채점이 변경된 경우일 수 있습니다.
                          </div>
                        )}
                      </div>
                    </li>
                  );
                })}
              </ul>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
