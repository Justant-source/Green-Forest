"use client";

import { toMediaUrl } from "@/lib/api";
import { countCompletedLines } from "@/lib/events/bingo-rules";
import type { PhotoBingoMarkerPayload } from "@/lib/events/postMarker";

interface Props {
  payload: PhotoBingoMarkerPayload;
}

function badgeFor(status: string, scored: boolean) {
  if (!scored) return null;
  if (status === "APPROVED") return { emoji: "✅", cls: "bg-green-500" };
  if (status === "REJECTED") return { emoji: "❌", cls: "bg-red-500" };
  return { emoji: "⏳", cls: "bg-gray-400" };
}

export default function BingoBoardView({ payload }: Props) {
  const scored = payload.eventStatus === "ENDED" || payload.eventStatus === "SCORED";
  const orderedCells = [...payload.cells].sort((a, b) => a.idx - b.idx);
  const uploaded = orderedCells.filter((c) => c.imageUrl).length;

  // 채점 완료된 경우에는 approvedCells 기준, 그 외에는 업로드된 셀 기준
  const approved = new Set<number>();
  orderedCells.forEach((c) => {
    if (c.scoreStatus === "APPROVED") approved.add(c.idx);
  });
  const displayLines = scored ? countCompletedLines(approved) : 0;

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-xs text-gray-600">
        <span>
          <b>{payload.eventTitle}</b> 참여
        </span>
        <span>
          업로드 {uploaded}/9
          {scored && (
            <>
              {" · "}
              <b className="text-forest-700">{payload.achievedLines ?? displayLines}줄</b>
              {payload.finalRewardDrops > 0 && (
                <> · 💧{payload.finalRewardDrops} 획득</>
              )}
            </>
          )}
        </span>
      </div>
      <div className="grid grid-cols-3 gap-1.5">
        {orderedCells.map((cell) => {
          const b = badgeFor(cell.scoreStatus, scored);
          return (
            <div
              key={cell.idx}
              className="relative aspect-square rounded-lg overflow-hidden bg-gray-100 border border-gray-200"
              title={cell.theme}
            >
              {cell.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={toMediaUrl(cell.imageUrl, "sm")}
                  alt={cell.theme}
                  loading="lazy"
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center p-1">
                  <span className="text-[10px] text-gray-500 text-center leading-tight break-keep">
                    {cell.theme}
                  </span>
                </div>
              )}
              {cell.imageUrl && (
                <div className="absolute inset-x-0 bottom-0 bg-black/40 text-white text-[9px] leading-tight text-center px-0.5 py-0.5 break-keep">
                  {cell.theme}
                </div>
              )}
              {b && (
                <div
                  className={`absolute top-1 right-1 w-5 h-5 rounded-full ${b.cls} text-white text-[10px] flex items-center justify-center shadow`}
                >
                  {b.emoji}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
