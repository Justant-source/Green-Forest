"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { listEvents } from "@/lib/events/api";
import type { Event } from "@/lib/events/types";

function statusBadge(s: Event["status"]) {
  const map: Record<Event["status"], { label: string; cls: string }> = {
    DRAFT: { label: "초안", cls: "bg-gray-100 text-gray-700" },
    SCHEDULED: { label: "예정", cls: "bg-blue-100 text-blue-700" },
    ACTIVE: { label: "진행 중", cls: "bg-forest-100 text-forest-700" },
    ENDED: { label: "종료 / 채점 대기", cls: "bg-orange-100 text-orange-800" },
    SCORED: { label: "완료", cls: "bg-purple-100 text-purple-800" },
  };
  const v = map[s];
  return <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${v.cls}`}>{v.label}</span>;
}

export default function EventsPage() {
  const [events, setEvents] = useState<Event[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listEvents("SCHEDULED,ACTIVE,ENDED,SCORED")
      .then(setEvents)
      .catch((e) => setError(e?.message ?? "불러오기 실패"));
  }, []);

  if (error) return <div className="text-red-500 text-center py-10">{error}</div>;
  if (!events) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }
  if (events.length === 0) {
    return <div className="text-center text-gray-500 py-10">진행 중인 이벤트가 없습니다.</div>;
  }

  return (
    <div className="space-y-3">
      <h1 className="text-lg font-bold">이벤트 퀘스트</h1>
      {events.map((e) => (
        <Link
          key={e.id}
          href={`/events/${e.id}`}
          className="block bg-white border border-gray-200 hover:border-forest-300 rounded-xl p-4"
        >
          <div className="flex items-center justify-between">
            <div className="font-semibold text-gray-900">{e.title}</div>
            {statusBadge(e.status)}
          </div>
          {e.description && <div className="text-sm text-gray-600 mt-1">{e.description}</div>}
          <div className="text-xs text-gray-500 mt-2">
            {e.startAt?.replace("T", " ")} ~ {e.endAt?.replace("T", " ")}
          </div>
        </Link>
      ))}
    </div>
  );
}
