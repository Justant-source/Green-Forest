"use client";

import Link from "next/link";
import { useEventMode } from "@/context/EventModeContext";

function pad(n: number) {
  return n.toString().padStart(2, "0");
}

function formatRemaining(ms: number): {
  days: string;
  hours: string;
  minutes: string;
  totalSec: number;
} {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const days = Math.floor(totalSec / 86400);
  const hours = Math.floor((totalSec % 86400) / 3600);
  const minutes = Math.floor((totalSec % 3600) / 60);
  return {
    days: pad(days),
    hours: pad(hours),
    minutes: pad(minutes),
    totalSec,
  };
}

export default function EventModeBanner() {
  const { mode, event, now } = useEventMode();
  if (mode !== "ACTIVE" || !event || !now) return null;

  const phase = event.phase;
  const exhibition = event.type === "PHOTO_EXHIBITION";
  // 잠금/종료 단계에서는 배너 숨김
  if (exhibition && phase !== "SUBMISSION" && phase !== "VOTING") return null;

  const deadline =
    exhibition && phase === "SUBMISSION" && event.photoExhibitionConfig?.submissionEnd
      ? event.photoExhibitionConfig.submissionEnd
      : exhibition && phase === "VOTING" && event.photoExhibitionConfig?.votingEnd
        ? event.photoExhibitionConfig.votingEnd
        : event.endAt;

  const endMs = new Date(deadline).getTime();
  const { days, hours, minutes, totalSec } = formatRemaining(endMs - now.getTime());
  const urgent = totalSec <= 3600;
  const voting = phase === "VOTING";
  const cta = voting ? "투표하러 가기 →" : "이벤트 참여하기 →";
  const label = voting ? "투표 마감까지" : exhibition ? "출품 마감까지" : "종료까지";

  return (
    <div
      className={`rounded-2xl p-5 border shadow-sm transition-colors ${
        urgent
          ? "bg-red-500 border-red-600 text-white animate-pulse"
          : voting
            ? "bg-amber-500 border-amber-600 text-white"
            : "bg-forest-500 border-forest-600 text-white"
      }`}
    >
      <div className="text-sm font-semibold opacity-90">
        {voting ? "사진전 투표" : event.title}
      </div>
      {voting && (
        <p className="mt-1 text-center text-xs opacity-90">익명 작품 · 최대 3개 선택</p>
      )}
      <div className="mt-1 text-center text-xs opacity-80">{label}</div>
      <div className="py-3">
        <div className="mx-auto flex max-w-xs items-start justify-center gap-1 font-mono sm:max-w-sm sm:gap-2">
          {[
            { value: days, label: "일" },
            { value: hours, label: "시간" },
            { value: minutes, label: "분" },
          ].map((unit, i) => (
            <div key={unit.label} className="flex items-start gap-1 sm:gap-2">
              {i > 0 && (
                <span className="pt-1 text-2xl font-semibold leading-none sm:pt-2 sm:text-3xl">:</span>
              )}
              <div className="flex min-w-[3.25rem] flex-col items-center sm:min-w-[3.75rem]">
                <span className="text-4xl font-bold leading-none sm:text-5xl">{unit.value}</span>
                <span className="mt-1 text-xs opacity-80 tracking-wide">{unit.label}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
      <Link
        href={`/events/${event.id}`}
        className="block text-center bg-white/95 text-forest-700 hover:bg-white rounded-full py-2 text-sm font-semibold"
      >
        {cta}
      </Link>
    </div>
  );
}
