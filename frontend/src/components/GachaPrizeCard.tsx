"use client";
import React from "react";
import { GachaPrizeInfo } from "@/types";

interface Props {
  prize: GachaPrizeInfo;
  onDraw: (prizeId: number) => void;
  disabled?: boolean;
  remainingDraws?: number;
}

const TIER_COLOR: Record<string, string> = {
  COMMON: "border-gray-300 bg-gray-50",
  RARE: "border-blue-300 bg-blue-50",
  EPIC: "border-purple-300 bg-purple-50",
  LEGENDARY: "border-yellow-400 bg-yellow-50",
};

const TIER_BADGE: Record<string, string> = {
  COMMON: "bg-gray-200 text-gray-700",
  RARE: "bg-blue-200 text-blue-800",
  EPIC: "bg-purple-200 text-purple-800",
  LEGENDARY: "bg-yellow-300 text-yellow-900",
};

export default function GachaPrizeCard({
  prize,
  onDraw,
  disabled = false,
  remainingDraws = 0,
}: Props) {
  const probPercent = (prize.currentProbability * 100).toFixed(2);
  const canDraw = !disabled && remainingDraws > 0 && prize.remainingStock > 0;

  return (
    <div
      className={`border-2 rounded-xl p-4 ${TIER_COLOR[prize.tier] ?? TIER_COLOR.COMMON}`}
    >
      <div className="flex items-start justify-between mb-2">
        <div>
          <span
            className={`text-xs px-2 py-0.5 rounded-full font-semibold ${TIER_BADGE[prize.tier]}`}
          >
            {prize.tierLabel}
          </span>
          <h3 className="font-bold text-gray-800 mt-1">{prize.name}</h3>
        </div>
        {prize.imageUrl && (
          <img
            src={prize.imageUrl}
            alt={prize.name}
            className="w-14 h-14 rounded-xl object-cover flex-shrink-0 border border-black/10"
          />
        )}
      </div>

      {prize.description && (
        <p className="text-sm text-gray-500 mb-2">{prize.description}</p>
      )}

      <div className="flex justify-between text-xs text-gray-500 mb-3">
        <span>현금가치: {prize.cashValue.toLocaleString()}원</span>
        <span>재고: {prize.remainingStock}개</span>
        <span>당첨확률: {probPercent}%</span>
      </div>

      <button
        onClick={() => onDraw(prize.id)}
        disabled={!canDraw}
        className={`w-full py-2 rounded-lg font-semibold text-sm transition-colors
          ${
            canDraw
              ? "bg-green-500 hover:bg-green-600 text-white"
              : "bg-gray-200 text-gray-400 cursor-not-allowed"
          }
        `}
      >
        {prize.remainingStock === 0
          ? "품절"
          : remainingDraws === 0
            ? "오늘 제한 초과"
            : "💧 30 뽑기"}
      </button>
    </div>
  );
}
