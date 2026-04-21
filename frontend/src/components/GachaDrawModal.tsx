"use client";
import React, { useState } from "react";
import { GachaDrawResult, GachaPrizeInfo } from "@/types";

interface Props {
  prize: GachaPrizeInfo | null;
  onConfirm: () => Promise<GachaDrawResult>;
  onClose: () => void;
}

type Phase = "confirm" | "spinning" | "result";

const SLOT_FRAMES = ["🎰", "🌟", "💫", "✨", "🎁", "🎊", "🏆", "🌈"];

export default function GachaDrawModal({ prize, onConfirm, onClose }: Props) {
  const [phase, setPhase] = useState<Phase>("confirm");
  const [result, setResult] = useState<GachaDrawResult | null>(null);
  const [slotIdx, setSlotIdx] = useState(0);
  const [error, setError] = useState("");

  const handleDraw = async () => {
    setPhase("spinning");
    // 슬롯 애니메이션 3초
    let frame = 0;
    const interval = setInterval(() => {
      setSlotIdx((frame++) % SLOT_FRAMES.length);
    }, 150);

    try {
      const res = await onConfirm();
      setTimeout(() => {
        clearInterval(interval);
        setResult(res);
        setPhase("result");
      }, 3000);
    } catch (e: any) {
      clearInterval(interval);
      const msg =
        e.status === 400
          ? await e
              .json()
              .then((d: any) => d.message ?? "오류가 발생했습니다")
          : "오류가 발생했습니다";
      setError(msg);
      setPhase("confirm");
    }
  };

  if (!prize) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={phase === "result" ? onClose : undefined}
    >
      <div
        className="bg-white rounded-2xl p-6 w-80 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {phase === "confirm" && (
          <>
            <h2 className="text-lg font-bold text-center mb-2">뽑기 확인</h2>
            <p className="text-center text-gray-600 mb-1">{prize.name}</p>
            <p className="text-center text-sm text-gray-400 mb-4">
              💧 30 물방울 차감
            </p>
            {error && (
              <p className="text-red-500 text-sm text-center mb-3">{error}</p>
            )}
            <div className="flex gap-2">
              <button
                onClick={onClose}
                className="flex-1 py-2 border rounded-lg text-gray-600 hover:bg-gray-50"
              >
                취소
              </button>
              <button
                onClick={handleDraw}
                className="flex-1 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 font-semibold"
              >
                뽑기!
              </button>
            </div>
          </>
        )}

        {phase === "spinning" && (
          <div className="text-center py-8">
            <div className="text-6xl mb-4 animate-bounce">
              {SLOT_FRAMES[slotIdx]}
            </div>
            <p className="text-gray-500">뽑는 중...</p>
          </div>
        )}

        {phase === "result" && result && (
          <div className="text-center">
            <div className="text-5xl mb-3">
              {result.isWinner ? "🎊" : "😢"}
            </div>
            <h2
              className={`text-xl font-bold mb-2 ${
                result.isWinner ? "text-yellow-600" : "text-gray-600"
              }`}
            >
              {result.isWinner ? "당첨!" : "아쉽게 꽝"}
            </h2>
            {result.isWinner && (
              <p className="text-gray-700 mb-1">{result.prizeName}</p>
            )}
            <p className="text-xs text-gray-400 mb-4">
              확률: {(result.probability * 100).toFixed(2)}% | 오늘 남은 뽑기:{" "}
              {result.remainingDrawsToday}회
            </p>
            <button
              onClick={onClose}
              className="w-full py-2 bg-green-500 text-white rounded-lg font-semibold"
            >
              닫기
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
