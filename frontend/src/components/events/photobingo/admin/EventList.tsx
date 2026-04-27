"use client";

import { useState } from "react";
import { adminChangeStatus, adminExtendEvent, adminFinalizeEvent } from "@/lib/events/api";
import type { Event, EventStatus } from "@/lib/events/types";
import BingoRewardsSummary from "./BingoRewardsSummary";

interface Props {
  events: Event[];
  onRefresh: () => void;
  onSelect: (eventId: number) => void;
}

const NEXT_STATUS: Record<EventStatus, EventStatus | null> = {
  DRAFT: "SCHEDULED",
  SCHEDULED: "ACTIVE",
  ACTIVE: "ENDED",
  ENDED: null, // finalize 버튼 별도
  SCORED: null,
};

const STATUS_COLOR: Record<EventStatus, string> = {
  DRAFT: "bg-gray-100 text-gray-700",
  SCHEDULED: "bg-blue-100 text-blue-700",
  ACTIVE: "bg-forest-100 text-forest-700",
  ENDED: "bg-orange-100 text-orange-800",
  SCORED: "bg-purple-100 text-purple-800",
};

export default function EventList({ events, onRefresh, onSelect }: Props) {
  const [summaryEvent, setSummaryEvent] = useState<Event | null>(null);

  const handleChange = async (id: number, status: EventStatus) => {
    if (!confirm(`상태를 ${status}로 변경할까요?`)) return;
    try {
      await adminChangeStatus(id, status);
      onRefresh();
    } catch (e: any) {
      alert(e?.message ?? "변경 실패");
    }
  };

  const handleExtend = async (id: number) => {
    const s = prompt("몇 분 연장할까요?", "10");
    if (!s) return;
    const mins = Number(s);
    if (!Number.isFinite(mins) || mins <= 0) return alert("1 이상 숫자를 입력하세요.");
    try {
      await adminExtendEvent(id, mins);
      onRefresh();
    } catch (e: any) {
      alert(e?.message ?? "연장 실패");
    }
  };

  const handleFinalize = async (id: number) => {
    if (!confirm("정말 최종 집계 & 물방울 지급을 진행할까요? 모든 셀 채점이 완료되어야 합니다.")) return;
    try {
      await adminFinalizeEvent(id);
      alert("✅ 물방울 지급 완료!");
      onRefresh();
    } catch (e: any) {
      alert(e?.message ?? "finalize 실패");
    }
  };

  if (events.length === 0) {
    return <div className="text-sm text-gray-500 text-center py-6">이벤트가 없습니다.</div>;
  }

  return (
    <div className="space-y-2">
      {events.map((e) => {
        const next = NEXT_STATUS[e.status];
        return (
          <div key={e.id} className="bg-white border border-gray-200 rounded-xl p-3">
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${STATUS_COLOR[e.status]}`}>
                    {e.status}
                  </span>
                  <div className="font-semibold text-sm">{e.title}</div>
                </div>
                <div className="text-[11px] text-gray-500 mt-1">
                  {e.startAt?.replace("T", " ")} ~ {e.endAt?.replace("T", " ")}
                </div>
              </div>
            </div>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {next && (
                <button
                  onClick={() => handleChange(e.id, next)}
                  className="px-2.5 py-1 rounded-full bg-forest-500 hover:bg-forest-600 text-white text-xs"
                >
                  → {next}
                </button>
              )}
              {e.status === "ACTIVE" && (
                <button
                  onClick={() => handleExtend(e.id)}
                  className="px-2.5 py-1 rounded-full bg-yellow-500 hover:bg-yellow-600 text-white text-xs"
                >
                  연장
                </button>
              )}
              {e.status === "ENDED" && (
                <button
                  onClick={() => onSelect(e.id)}
                  className="px-2.5 py-1 rounded-full bg-blue-500 hover:bg-blue-600 text-white text-xs"
                >
                  채점
                </button>
              )}
              {e.status === "ENDED" && (
                <button
                  onClick={() => handleFinalize(e.id)}
                  className="px-2.5 py-1 rounded-full bg-purple-500 hover:bg-purple-600 text-white text-xs"
                >
                  최종 집계 & 지급
                </button>
              )}
              {e.status === "SCORED" && (
                <button
                  onClick={() => setSummaryEvent(e)}
                  className="px-2.5 py-1 rounded-full bg-purple-500 hover:bg-purple-600 text-white text-xs"
                >
                  지급 결과
                </button>
              )}
              {e.status === "ACTIVE" && (
                <button
                  onClick={() => onSelect(e.id)}
                  className="px-2.5 py-1 rounded-full bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs"
                >
                  현황 보기
                </button>
              )}
            </div>
          </div>
        );
      })}
      {summaryEvent && (
        <BingoRewardsSummary event={summaryEvent} onClose={() => setSummaryEvent(null)} />
      )}
    </div>
  );
}
