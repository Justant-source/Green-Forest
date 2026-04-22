"use client";
import React, { useEffect, useState } from "react";
import { PlazaWinner } from "@/types";
import { getPlazaWinners, getActiveAnnouncement, AnnouncementItem } from "@/lib/api";

function formatRelativeTime(createdAt: string): string {
  const diff = Math.floor((Date.now() - new Date(createdAt).getTime()) / 1000);
  if (diff < 3600) return `${Math.max(1, Math.floor(diff / 60))}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  return `${Math.floor(diff / 86400)}일 전`;
}

type TickerItem =
  | { kind: "winner"; data: PlazaWinner }
  | { kind: "announcement"; data: AnnouncementItem };

export default function GachaRecentWinsTicker() {
  const [items, setItems] = useState<TickerItem[]>([]);
  const [idx, setIdx] = useState(0);
  const [visible, setVisible] = useState(true);
  const [popup, setPopup] = useState<AnnouncementItem | null>(null);

  useEffect(() => {
    const load = async () => {
      const [winners, ann] = await Promise.all([
        getPlazaWinners().catch(() => [] as PlazaWinner[]),
        getActiveAnnouncement(),
      ]);
      const list: TickerItem[] = [];
      if (ann) list.push({ kind: "announcement", data: ann });
      winners.forEach((w) => list.push({ kind: "winner", data: w }));
      setItems(list);
    };
    load();
  }, []);

  useEffect(() => {
    if (items.length <= 1) return;
    const t = setInterval(() => {
      setVisible(false);
      setTimeout(() => {
        setIdx((i) => (i + 1) % items.length);
        setVisible(true);
      }, 300);
    }, 2500);
    return () => clearInterval(t);
  }, [items]);

  if (items.length === 0) return null;

  const item = items[idx];

  const renderContent = () => {
    if (item.kind === "announcement") {
      return (
        <button
          onClick={() => setPopup(item.data)}
          className="flex items-baseline gap-2 min-w-0 text-left hover:opacity-80 transition-opacity"
        >
          <span className="truncate text-gray-700">
            <span className="mr-1">📢</span>
            <strong>{item.data.title}</strong>
          </span>
          <span className="shrink-0 text-blue-400 text-xs underline">자세히</span>
        </button>
      );
    }
    const w = item.data;
    const icon = w.type === "GACHA" ? "🎊" : "🎉";
    const text = w.type === "GACHA"
      ? `${w.userNickname}님이 ${w.prizeName} 당첨!`
      : (() => {
          const date = new Date(w.checkinDate + "T00:00:00");
          return `${w.userNickname}님이 ${date.getMonth()+1}월 ${date.getDate()}일 출석 이벤트 당첨!`;
        })();
    return (
      <div className="flex items-baseline gap-2 min-w-0">
        <span className="truncate text-gray-600">
          <span className="mr-1">{icon}</span>
          <strong>{text}</strong>
        </span>
        <span className="shrink-0 text-gray-400" style={{ fontSize: "0.67em" }}>
          {formatRelativeTime(w.createdAt)}
        </span>
      </div>
    );
  };

  const isAnn = item.kind === "announcement";

  return (
    <>
      <div className={`border rounded-lg px-3 py-2 flex items-center gap-2 text-sm overflow-hidden h-9 ${
        isAnn ? "bg-blue-50 border-blue-200" : "bg-yellow-50 border-yellow-200"
      }`}>
        <span className={`font-bold shrink-0 ${isAnn ? "text-blue-500" : "text-yellow-500"}`}>
          {isAnn ? "공지" : "당첨"}
        </span>
        <div
          className="flex-1 min-w-0 transition-all duration-300"
          style={{
            transform: visible ? "translateY(0)" : "translateY(100%)",
            opacity: visible ? 1 : 0,
          }}
        >
          {renderContent()}
        </div>
      </div>

      {/* 공지 팝업 */}
      {popup && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
          onClick={() => setPopup(null)}
        >
          <div
            className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between mb-3">
              <h2 className="text-base font-bold text-gray-800 flex items-center gap-1.5">
                <span>📢</span> {popup.title}
              </h2>
              <button onClick={() => setPopup(null)} className="text-gray-400 hover:text-gray-600 text-xl leading-none ml-2">✕</button>
            </div>
            <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">{popup.content}</p>
            <p className="text-xs text-gray-400 mt-4">
              {new Date(popup.createdAt).toLocaleDateString("ko-KR")}
            </p>
          </div>
        </div>
      )}
    </>
  );
}
