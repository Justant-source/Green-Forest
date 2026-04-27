"use client";

import Link from "next/link";
import { useEventMode } from "@/context/EventModeContext";

function pad(n: number) {
  return n.toString().padStart(2, "0");
}

function formatRemaining(ms: number): { mm: string; ss: string; totalSec: number } {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const mm = pad(Math.floor(totalSec / 60));
  const ss = pad(totalSec % 60);
  return { mm, ss, totalSec };
}

export default function EventModeBanner() {
  const { mode, event, now } = useEventMode();
  if (mode !== "ACTIVE" || !event || !now) return null;

  const endMs = new Date(event.endAt).getTime();
  const { mm, ss, totalSec } = formatRemaining(endMs - now.getTime());
  const urgent = totalSec <= 60;

  return (
    <div
      className={`rounded-2xl p-5 border shadow-sm transition-colors ${
        urgent ? "bg-red-500 border-red-600 text-white animate-pulse" : "bg-forest-500 border-forest-600 text-white"
      }`}
    >
      <div className="text-sm font-semibold opacity-90">
        {event.title}
      </div>
      <div className="text-center py-3">
        <div className="flex items-baseline justify-center gap-1 font-mono">
          <span className="text-5xl font-bold leading-none">{mm}</span>
          <span className="text-2xl font-semibold mx-1">:</span>
          <span className="text-5xl font-bold leading-none">{ss}</span>
        </div>
        <div className="mt-1 text-xs opacity-80 tracking-wide">분 &nbsp;&nbsp; 초</div>
      </div>
      <Link
        href={`/events/${event.id}`}
        className="block text-center bg-white/95 text-forest-700 hover:bg-white rounded-full py-2 text-sm font-semibold"
      >
        이벤트 참여하기 →
      </Link>
    </div>
  );
}
