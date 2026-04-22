"use client";
import React, { useEffect, useState, useCallback } from "react";
import { TodayBoard, AttendancePhrase } from "@/types";
import { checkin, getTodayBoard, getRandomPhrases } from "@/lib/api";
import AttendanceBoard from "@/components/AttendanceBoard";
import AttendanceStamp from "@/components/AttendanceStamp";
import PhrasePicker from "@/components/PhrasePicker";

export default function AttendancePage() {
  const [board, setBoard] = useState<TodayBoard | null>(null);
  const [phrases, setPhrases] = useState<AttendancePhrase[]>([]);
  const [selectedMessage, setSelectedMessage] = useState("");
  const [selectedPhraseId, setSelectedPhraseId] = useState<number | undefined>();
  const [stamped, setStamped] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<any>(null);

  const loadBoard = useCallback(async () => {
    try {
      const b = await getTodayBoard();
      setBoard(b);
    } catch {}
  }, []);

  useEffect(() => {
    loadBoard();
    getRandomPhrases(5).then(setPhrases).catch(() => {});
  }, [loadBoard]);

  // 날짜 변경(6시 이후) 감지: 1분마다 board.date와 오늘 비교, 탭 포커스 시에도 확인
  useEffect(() => {
    const todayStr = () => new Date().toLocaleDateString("sv-SE"); // YYYY-MM-DD

    const checkAndRefresh = () => {
      if (board && board.date !== todayStr()) {
        setBoard(null);
        setResult(null);
        setStamped(false);
        loadBoard();
      }
    };

    const interval = setInterval(checkAndRefresh, 60_000);
    window.addEventListener("focus", checkAndRefresh);
    return () => {
      clearInterval(interval);
      window.removeEventListener("focus", checkAndRefresh);
    };
  }, [board, loadBoard]);

  const handleSelect = (value: string, phraseId?: number) => {
    setSelectedMessage(value);
    setSelectedPhraseId(phraseId);
  };

  const handleStamp = async () => {
    if (!selectedMessage && !selectedPhraseId) {
      setError("한마디를 선택하거나 직접 입력해주세요");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const r = await checkin({
        message: selectedPhraseId ? undefined : selectedMessage,
        phraseId: selectedPhraseId,
      });
      setResult(r);
      setStamped(true);
      await loadBoard();
    } catch (e: any) {
      if (e.status === 409) setError("오늘 이미 출석하셨습니다");
      else if (e.status === 422)
        setError("출석 가능 시간(평일 06:00~11:00)이 아닙니다");
      else setError("출석 중 오류가 발생했습니다");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      <div className="max-w-lg mx-auto px-4 pt-6 space-y-5">
        {/* 출석 보드 */}
        <AttendanceBoard board={board} />

        {/* 출석 섹션 */}
        <div className="bg-white rounded-xl p-5 shadow-sm space-y-4">
          <h2 className="font-semibold text-gray-700">출석하기</h2>

          {result ? (
            <div className="text-center py-3">
              <p className="text-green-600 font-semibold">✅ 출석 완료!</p>
              <p className="text-sm text-gray-500 mt-1">
                💧 {result.dropsAwarded} 물방울 획득
              </p>
              <p className="text-xs text-gray-400">
                이번 달 {result.monthCheckinCount}일 출석 / 연속 {result.streak}일
              </p>
            </div>
          ) : (
            <>
              <PhrasePicker phrases={phrases} onSelect={handleSelect} />
              {error && <p className="text-red-500 text-sm">{error}</p>}
              <div className="flex justify-center pt-2">
                <AttendanceStamp
                  onStamp={handleStamp}
                  disabled={loading}
                  stamped={stamped}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
