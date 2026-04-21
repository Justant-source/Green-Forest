"use client";
import React from "react";
import { AttendanceDay } from "@/types";

interface Props {
  days: AttendanceDay[];
  year: number;
  month: number; // 1-12
}

export default function AttendanceCalendar({ days, year, month }: Props) {
  const dayMap = new Map(days.map((d) => [d.date, d]));
  const firstDay = new Date(year, month - 1, 1).getDay(); // 0=Sun
  const daysInMonth = new Date(year, month, 0).getDate();

  const cells: (AttendanceDay | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => {
      const dateStr = `${year}-${String(month).padStart(2, "0")}-${String(i + 1).padStart(
        2,
        "0"
      )}`;
      return (
        dayMap.get(dateStr) ?? { date: dateStr, isWinner: false, message: "" }
      );
    }),
  ];

  const DAYS = ["일", "월", "화", "수", "목", "금", "토"];

  return (
    <div>
      <div className="grid grid-cols-7 gap-1 mb-1">
        {DAYS.map((d) => (
          <div key={d} className="text-center text-xs text-gray-400">
            {d}
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {cells.map((cell, i) => {
          if (!cell)
            return (
              <div key={i} className="aspect-square" />
            );
          const date = parseInt(cell.date.split("-")[2]);
          const hasCheckin = dayMap.has(cell.date);
          const isWinner = cell.isWinner;
          return (
            <div
              key={i}
              title={cell.message || ""}
              className={`aspect-square rounded flex items-center justify-center text-xs font-medium
                ${
                  isWinner
                    ? "bg-yellow-400 text-white"
                    : hasCheckin
                      ? "bg-green-400 text-white"
                      : "bg-gray-100 text-gray-500"
                }
              `}
            >
              {date}
            </div>
          );
        })}
      </div>
      <div className="flex gap-3 mt-2 text-xs text-gray-400">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-green-400 inline-block" /> 출석
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-yellow-400 inline-block" /> 당첨
        </span>
      </div>
    </div>
  );
}
