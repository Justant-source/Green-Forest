"use client";

import { toMediaUrl } from "@/lib/api";
import type { PhotoBingoCellDto, EventStatus } from "@/lib/events/types";

interface Props {
  cell: PhotoBingoCellDto;
  /** 낙관적 UI용 로컬 preview URL (업로드 중). */
  localPreview?: string | null;
  uploading?: boolean;
  eventStatus: EventStatus;
  onTap: () => void;
  onDelete: () => void;
}

function scoreBadge(status: PhotoBingoCellDto["scoreStatus"]) {
  if (status === "APPROVED") return { emoji: "✅", cls: "bg-green-500" };
  if (status === "REJECTED") return { emoji: "❌", cls: "bg-red-500" };
  return { emoji: "⏳", cls: "bg-gray-400" };
}

export default function BingoCell({
  cell,
  localPreview,
  uploading,
  eventStatus,
  onTap,
  onDelete,
}: Props) {
  const scored = eventStatus === "ENDED" || eventStatus === "SCORED";
  const displayUrl = localPreview ?? (cell.imageUrl ? toMediaUrl(cell.imageUrl, "sm") : null);

  // 빈 셀
  if (!displayUrl && !uploading) {
    return (
      <button
        type="button"
        onClick={onTap}
        disabled={eventStatus !== "ACTIVE"}
        className={`aspect-square w-full rounded-xl border border-gray-200 bg-gray-100 flex flex-col items-center justify-center text-center p-1.5 transition ${
          eventStatus === "ACTIVE"
            ? "hover:bg-gray-200 active:scale-95"
            : "opacity-60 cursor-not-allowed"
        }`}
      >
        <div className="text-2xl mb-1">📷</div>
        <div className="text-[10px] leading-tight text-gray-700 line-clamp-3 break-keep">
          {cell.theme}
        </div>
      </button>
    );
  }

  // 업로드 중 스피너
  if (uploading) {
    return (
      <div className="aspect-square w-full rounded-xl bg-gray-100 flex flex-col items-center justify-center">
        <div className="w-8 h-8 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
        <div className="text-[10px] text-gray-500 mt-2">업로드 중</div>
      </div>
    );
  }

  // 업로드/채점 완료 상태
  const badge = scored ? scoreBadge(cell.scoreStatus) : null;

  return (
    <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-gray-100 border border-gray-200">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={displayUrl!} alt={cell.theme} className="w-full h-full object-cover" />
      {!scored && eventStatus === "ACTIVE" && (
        <button
          type="button"
          onClick={onDelete}
          aria-label="사진 삭제"
          className="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/60 text-white text-xs flex items-center justify-center"
        >
          ✕
        </button>
      )}
      {badge && (
        <div
          className={`absolute bottom-1 right-1 w-6 h-6 rounded-full ${badge.cls} text-white text-xs flex items-center justify-center shadow`}
          title={cell.scoreComment || undefined}
        >
          {badge.emoji}
        </div>
      )}
    </div>
  );
}
