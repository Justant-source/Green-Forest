"use client";
import React from "react";

interface Props {
  stampStyle: string;
  winner?: boolean;
  size?: number;
}

// 성장 단계 이모지: 0=씨앗, 1=새싹, 2=잎2장, 3=꽃봉오리, 4=꽃, 5=열매
const STAGE_EMOJI = ["🌱", "🌿", "🍃", "🌸", "🌺", "🍎"];
const STAGE_COLOR = [
  "bg-green-100",
  "bg-emerald-100",
  "bg-lime-100",
  "bg-pink-100",
  "bg-rose-100",
  "bg-amber-100",
];

// 구형 stampStyle(plantType_jobClass) 하위호환용
const LEGACY_PLANT_EMOJI: Record<string, string> = {
  TABLE: "🌴",
  SPATHIPHYLLUM: "🌿",
  HONG: "🌱",
  ORANGE: "🌸",
  DEFAULT: "🌱",
};
const LEGACY_PLANT_COLOR: Record<string, string> = {
  TABLE: "bg-emerald-100",
  SPATHIPHYLLUM: "bg-teal-100",
  HONG: "bg-lime-100",
  ORANGE: "bg-orange-100",
  DEFAULT: "bg-green-100",
};

const JOB_EMOJI: Record<string, string> = {
  TANKER: "🛡️",
  HEALER: "💊",
  DEALER: "⚔️",
  BUFFER: "✨",
  SUPPORTER: "🤝",
  MAGE: "🔮",
  ARCHER: "🏹",
  DEFAULT: "⭐",
};

export default function StampIcon({ stampStyle, winner = false, size = 48 }: Props) {
  const parts = stampStyle.split("_");
  const stageNum = parseInt(parts[0], 10);
  const isNewFormat = !isNaN(stageNum) && parts.length === 2;
  const job = parts[parts.length - 1];
  const jobEmoji = JOB_EMOJI[job] ?? JOB_EMOJI.DEFAULT;

  let plantEmoji: string;
  let bgColor: string;
  if (isNewFormat) {
    const stage = Math.min(Math.max(stageNum, 0), 5);
    plantEmoji = STAGE_EMOJI[stage];
    bgColor = STAGE_COLOR[stage];
  } else {
    const plant = parts[0];
    plantEmoji = LEGACY_PLANT_EMOJI[plant] ?? LEGACY_PLANT_EMOJI.DEFAULT;
    bgColor = LEGACY_PLANT_COLOR[plant] ?? LEGACY_PLANT_COLOR.DEFAULT;
  }

  const crownSize = Math.round(size * 0.38);
  const crownOffset = Math.round(size * 0.28);

  return (
    <div style={{ width: size + (winner ? 16 : 0), height: size + (winner ? 16 : 0) }} className="relative flex items-center justify-center">
      {/* 폭죽 — 당첨자만 */}
      {winner && (
        <>
          <span
            style={{ fontSize: crownSize * 0.7, position: "absolute", top: -2, left: -4 }}
            className="animate-bounce"
          >
            🎉
          </span>
          <span
            style={{ fontSize: crownSize * 0.6, position: "absolute", bottom: 0, right: -4 }}
            className="animate-bounce"
            >
            🎊
          </span>
        </>
      )}

      {/* 도장 원 */}
      <div
        style={{ width: size, height: size }}
        className={`relative flex items-center justify-center rounded-full ${bgColor} text-center select-none
          ${winner ? "ring-[3px] ring-yellow-400 ring-offset-2 shadow-lg shadow-yellow-200" : ""}
        `}
      >
        <span style={{ fontSize: size * 0.45 }}>{plantEmoji}</span>
        <span
          style={{ fontSize: size * 0.25, position: "absolute", bottom: 0, right: 0 }}
          className="bg-white rounded-full"
        >
          {jobEmoji}
        </span>
      </div>

      {/* 왕관 — 당첨자만 */}
      {winner && (
        <span
          style={{
            fontSize: crownSize,
            position: "absolute",
            top: -crownOffset,
            left: "50%",
            transform: "translateX(-50%) rotate(-15deg)",
            lineHeight: 1,
            filter: "drop-shadow(0 1px 2px rgba(0,0,0,0.2))",
          }}
        >
          👑
        </span>
      )}
    </div>
  );
}
