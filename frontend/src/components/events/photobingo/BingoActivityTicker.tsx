"use client";

import { useEffect, useRef, useState } from "react";
import { getBingoActivity } from "@/lib/events/api";
import type { PhotoBingoActivity, EventStatus } from "@/lib/events/types";

interface Props {
  eventId: number;
  status: EventStatus;
  myUserId?: number | null;
  intervalMs?: number;
}

function ordinal(n: number): string {
  return `${n}번째`;
}

function timeAgo(iso: string, now: Date): string {
  const t = new Date(iso).getTime();
  const diff = Math.max(0, now.getTime() - t);
  const sec = Math.floor(diff / 1000);
  if (sec < 5) return "방금";
  if (sec < 60) return `${sec}초 전`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}분 전`;
  const hr = Math.floor(min / 60);
  return `${hr}시간 전`;
}

export default function BingoActivityTicker({ eventId, status, myUserId, intervalMs = 6000 }: Props) {
  const [items, setItems] = useState<PhotoBingoActivity[] | null>(null);
  const [cursor, setCursor] = useState(0);
  const [now, setNow] = useState(new Date());
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const refresh = async () => {
    try {
      const list = await getBingoActivity(eventId, 20);
      setItems(list);
      // 맨 앞(가장 최신)으로 이동
      setCursor(0);
    } catch {
      /* 조용히 무시 */
    }
  };

  useEffect(() => {
    refresh();
    pollRef.current = setInterval(refresh, intervalMs);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId, intervalMs]);

  // 3초 간격 자동 순환
  useEffect(() => {
    if (!items || items.length <= 1) return;
    timerRef.current = setInterval(() => {
      setCursor((c) => (c + 1) % items.length);
      setNow(new Date());
    }, 3000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [items]);

  // 매 초 now 갱신 (시간 표시용)
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  if (!items) return null;
  if (items.length === 0) {
    return (
      <div className="bg-forest-50 border border-forest-200 rounded-xl p-3 text-xs text-forest-700 text-center">
        아직 업로드한 사람이 없어요. 먼저 올려서 빙고를 시작해보세요.
      </div>
    );
  }

  const current = items[cursor];
  const mine = myUserId != null && current.userId === myUserId;

  return (
    <div className="bg-gradient-to-r from-forest-500 to-forest-600 text-white rounded-xl p-3 overflow-hidden">
      <div className="flex items-center justify-between text-[10px] uppercase tracking-wider opacity-80 mb-1">
        <span>다른 참여자 활동</span>
        <span>
          {cursor + 1}/{items.length}
        </span>
      </div>
      <div className="text-sm">
        <div className="truncate font-medium">
          <b>{mine ? "내" : current.userNickname}</b>가 {ordinal(current.uploadedCount)} 사진을 올렸어요!
        </div>
        <div className="text-[11px] opacity-80 truncate mt-0.5">
          <span className="opacity-80">{current.theme}</span>
          <span className="mx-1">·</span>
          <span>{timeAgo(current.uploadedAt, now)}</span>
          {status === "ACTIVE" && !mine && <span className="ml-1">· 나도 올려볼까?</span>}
        </div>
      </div>
    </div>
  );
}
