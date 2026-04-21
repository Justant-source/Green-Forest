"use client";
import React, { useEffect, useState } from "react";
import { PlazaWinner } from "@/types";
import { getPlazaWinners } from "@/lib/api";

function formatRelativeTime(createdAt: string): string {
  const diff = Math.floor((Date.now() - new Date(createdAt).getTime()) / 1000);
  if (diff < 3600) return `${Math.max(1, Math.floor(diff / 60))}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  return `${Math.floor(diff / 86400)}일 전`;
}

function formatWinnerLabel(w: PlazaWinner): { icon: string; text: string } {
  if (w.type === "GACHA") {
    return {
      icon: "🎊",
      text: `${w.userNickname}님이 ${w.prizeName} 당첨!`,
    };
  }
  const date = new Date(w.checkinDate + "T00:00:00");
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return {
    icon: "🎉",
    text: `${w.userNickname}님이 ${month}월 ${day}일 출석 이벤트 당첨!`,
  };
}

export default function GachaRecentWinsTicker() {
  const [winners, setWinners] = useState<PlazaWinner[]>([]);
  const [idx, setIdx] = useState(0);
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    getPlazaWinners().then(setWinners).catch(() => {});
  }, []);

  useEffect(() => {
    if (winners.length <= 1) return;
    const t = setInterval(() => {
      setVisible(false);
      setTimeout(() => {
        setIdx((i) => (i + 1) % winners.length);
        setVisible(true);
      }, 300);
    }, 2000);
    return () => clearInterval(t);
  }, [winners]);

  if (winners.length === 0) return null;

  const win = winners[idx];
  const { icon, text } = formatWinnerLabel(win);
  const relTime = formatRelativeTime(win.createdAt);

  return (
    <div className="bg-yellow-50 border border-yellow-200 rounded-lg px-3 py-2 flex items-center gap-2 text-sm overflow-hidden h-9">
      <span className="text-yellow-500 font-bold shrink-0">당첨</span>
      <div
        className="text-gray-600 flex items-baseline gap-2 min-w-0 transition-all duration-300"
        style={{
          transform: visible ? "translateY(0)" : "translateY(100%)",
          opacity: visible ? 1 : 0,
        }}
      >
        <span className="truncate">
          <span className="mr-1">{icon}</span>
          <strong>{text}</strong>
        </span>
        <span className="shrink-0 text-gray-400" style={{ fontSize: "0.67em" }}>
          {relTime}
        </span>
      </div>
    </div>
  );
}
