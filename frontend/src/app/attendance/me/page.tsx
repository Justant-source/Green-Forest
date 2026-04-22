"use client";
import React, { useEffect, useState } from "react";
import { AttendanceMonth, MyAttendanceWin } from "@/types";
import { getMyAttendanceMonth, getMyAttendanceWins } from "@/lib/api";
import AttendanceCalendar from "@/components/AttendanceCalendar";

const WIN_STATUS_LABEL: Record<string, string> = {
  PENDING: "전달 대기",
  DELIVERED: "전달 완료",
  NONE: "-",
};
const WIN_STATUS_STYLE: Record<string, string> = {
  PENDING: "bg-orange-100 text-orange-700 border-orange-300",
  DELIVERED: "bg-green-100 text-green-700 border-green-300",
  NONE: "bg-gray-100 text-gray-500 border-gray-300",
};

export default function MyAttendancePage() {
  const [data, setData] = useState<AttendanceMonth | null>(null);
  const [wins, setWins] = useState<MyAttendanceWin[]>([]);
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  });

  useEffect(() => {
    getMyAttendanceMonth(month).then(setData).catch(() => {});
  }, [month]);

  useEffect(() => {
    getMyAttendanceWins().then(setWins).catch(() => setWins([]));
  }, []);

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

        {/* 내 당첨 내역 */}
        <div className="bg-white rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-gray-700">내 당첨 내역</h2>
            <span className="text-xs text-gray-500">총 {wins.length}회</span>
          </div>
          {wins.length === 0 ? (
            <div className="text-center text-gray-400 text-sm py-4">아직 당첨된 적이 없습니다</div>
          ) : (
            <div className="space-y-2">
              {wins.map((w) => (
                <div key={w.id} className={`flex items-start justify-between gap-2 p-3 rounded-lg border-l-4 ${w.deliveryStatus === "DELIVERED" ? "border-green-400 bg-green-50" : "border-orange-400 bg-orange-50"}`}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-semibold text-gray-800">{w.date}</span>
                      <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${WIN_STATUS_STYLE[w.deliveryStatus]}`}>
                        {WIN_STATUS_LABEL[w.deliveryStatus]}
                      </span>
                    </div>
                    {w.message && <div className="text-xs text-gray-600 mt-1">한마디: {w.message}</div>}
                    {w.deliveryStatus === "DELIVERED" && w.deliveredAt && (
                      <div className="text-xs text-green-700 mt-1">
                        전달완료: {new Date(w.deliveredAt).toLocaleString("ko-KR")}
                        {w.deliveryMemo && <span className="ml-2 text-gray-500">메모: {w.deliveryMemo}</span>}
                      </div>
                    )}
                    {w.deliveryStatus === "PENDING" && (
                      <div className="text-xs text-orange-700 mt-1">
                        관리자가 보상을 준비 중입니다
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

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
