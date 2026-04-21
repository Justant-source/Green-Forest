"use client";
import React, { useState } from "react";
import { TodayBoard, TodayBoardEntry } from "@/types";
import StampIcon from "./StampIcon";

interface Props {
  board: TodayBoard | null;
}

export default function AttendanceBoard({ board }: Props) {
  const [tooltip, setTooltip] = useState<TodayBoardEntry | null>(null);

  if (!board) {
    return (
      <div className="p-4 bg-green-50 rounded-xl text-center text-gray-400 text-sm">
        출석 현황 불러오는 중...
      </div>
    );
  }

  const checkins = board.checkins;
  const totalSlots = Math.max(12, Math.ceil(checkins.length / 6) * 6);
  const slots = Array.from({ length: totalSlots });

  return (
    <div className="bg-white rounded-xl p-4 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <h2 className="font-semibold text-gray-700">오늘의 출석</h2>
        <span className="text-sm text-green-600">{checkins.length}명 출석</span>
      </div>

      <div className="grid grid-cols-6 gap-2">
        {slots.map((_, i) => {
          const entry = checkins[i];
          if (!entry) {
            return (
              <div
                key={i}
                className="aspect-square rounded-full bg-gray-100 opacity-30 flex items-center justify-center"
              >
                <span className="text-gray-300 text-lg">🌱</span>
              </div>
            );
          }

          return (
            <div
              key={i}
              className={`flex items-center justify-center cursor-pointer ${entry.isWinner ? "py-2" : "aspect-square"}`}
              onClick={() =>
                setTooltip(tooltip?.userId === entry.userId ? null : entry)
              }
            >
              <StampIcon stampStyle={entry.stampStyle} winner={entry.isWinner} size={44} />
            </div>
          );
        })}
      </div>

      {tooltip && (
        <div
          className="mt-3 p-3 bg-gray-50 rounded-lg text-sm border border-gray-200"
          onClick={() => setTooltip(null)}
        >
          <div className="font-semibold text-gray-700">
            {tooltip.isWinner && <span className="mr-1">👑</span>}
            {tooltip.nickname}
            {tooltip.isWinner && <span className="ml-1 text-yellow-500 text-xs">오늘의 당첨자!</span>}
          </div>
          <div className="text-gray-500 mt-1">"{tooltip.message}"</div>
          <div className="text-xs text-gray-400 mt-1">
            {new Date(tooltip.checkinAt).toLocaleTimeString("ko-KR", {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </div>
        </div>
      )}
    </div>
  );
}
