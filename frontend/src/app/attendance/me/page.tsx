"use client";
import React, { useEffect, useState } from "react";
import { AttendanceMonth } from "@/types";
import { getMyAttendanceMonth } from "@/lib/api";
import AttendanceCalendar from "@/components/AttendanceCalendar";

export default function MyAttendancePage() {
  const [data, setData] = useState<AttendanceMonth | null>(null);
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  });

  useEffect(() => {
    getMyAttendanceMonth(month).then(setData).catch(() => {});
  }, [month]);

  const changeMonth = (delta: number) => {
    const [y, m] = month.split("-").map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    setMonth(
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`
    );
  };

  const [year, mon] = month.split("-").map(Number);

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      <div className="max-w-lg mx-auto px-4 pt-6 space-y-4">
        <h1 className="text-xl font-bold text-gray-800">내 출석 기록</h1>

        <div className="bg-white rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <button
              onClick={() => changeMonth(-1)}
              className="p-2 hover:bg-gray-100 rounded-full"
            >
              ◀
            </button>
            <span className="font-semibold text-gray-700">
              {year}년 {mon}월
            </span>
            <button
              onClick={() => changeMonth(1)}
              className="p-2 hover:bg-gray-100 rounded-full"
            >
              ▶
            </button>
          </div>

          {data && (
            <>
              <div className="flex gap-4 mb-4 text-sm text-center">
                <div className="flex-1 bg-green-50 rounded-lg p-2">
                  <div className="font-bold text-green-700 text-lg">
                    {data.checkinCount}
                  </div>
                  <div className="text-gray-500">출석일</div>
                </div>
                <div className="flex-1 bg-yellow-50 rounded-lg p-2">
                  <div className="font-bold text-yellow-700 text-lg">
                    {data.winCount}
                  </div>
                  <div className="text-gray-500">당첨</div>
                </div>
                <div className="flex-1 bg-blue-50 rounded-lg p-2">
                  <div className="font-bold text-blue-700 text-lg">
                    {data.streak}
                  </div>
                  <div className="text-gray-500">연속</div>
                </div>
              </div>
              <AttendanceCalendar
                days={data.days}
                year={year}
                month={mon}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
