"use client";
import React, { useState } from "react";
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
  const [showBreakdown, setShowBreakdown] = useState(false);

  const probPercent = (prize.currentProbability * 100).toFixed(2);
  const canDraw = !disabled && remainingDraws > 0 && prize.remainingStock > 0;
  const bd = prize.probabilityBreakdown;
  const hasBonus = bd && (bd.pityStacks > 0 || bd.factors.length > 0);

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

      <div className="flex justify-between text-xs text-gray-500 mb-2">
        <span>현금가치: {prize.cashValue.toLocaleString()}원</span>
        <span>재고: {prize.remainingStock}개</span>
      </div>

      <div className="flex items-center justify-between mb-3">
        <span className="text-sm font-semibold text-gray-800">
          당첨확률: {probPercent}%
          {hasBonus && bd && (
            <span className="ml-1 text-xs font-normal text-green-600">
              (+{((bd.pityBonus + bd.activityBonus) * 100).toFixed(2)}%p 보너스)
            </span>
          )}
        </span>
        {bd && (
          <button
            onClick={() => setShowBreakdown(!showBreakdown)}
            className="text-xs text-blue-500 underline underline-offset-2"
          >
            {showBreakdown ? "접기" : "상세"}
          </button>
        )}
      </div>

      {showBreakdown && bd && (
        <div className="bg-white/70 rounded-lg px-3 py-2 mb-3 text-xs border border-gray-200 space-y-1">
          <div className="flex justify-between text-gray-600">
            <span>기본 확률</span>
            <span>{(bd.base * 100).toFixed(2)}%</span>
          </div>
          {bd.pityStacks > 0 && (
            <div className="flex justify-between text-orange-600">
              <span>미당첨 스택 ({bd.pityStacks}회)</span>
              <span>+{(bd.pityBonus * 100).toFixed(2)}%p</span>
            </div>
          )}
          {bd.factors.length > 0 && (
            <>
              <div className="border-t border-gray-200 pt-1 text-purple-700 font-semibold">
                개인 활동 보너스
              </div>
              {bd.factors.map((f, i) => (
                <div key={i} className="flex justify-between text-purple-600 pl-2">
                  <span>{f.label}</span>
                  <span>+{(f.bonus * 100).toFixed(2)}%p</span>
                </div>
              ))}
            </>
          )}
          <div className="flex justify-between font-bold text-gray-800 border-t border-gray-200 pt-1">
            <span>최종 확률</span>
            <span>{(bd.total * 100).toFixed(2)}%</span>
          </div>
        </div>
      )}

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
