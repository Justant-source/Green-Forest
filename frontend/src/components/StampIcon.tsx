"use client";
import React from "react";

interface Props {
  stampStyle: string;
  winner?: boolean;
  size?: number;
}

const PLANT_EMOJI: Record<string, string> = {
  CACTUS: "🌵",
  ROSE: "🌹",
  SUNFLOWER: "🌻",
  TULIP: "🌷",
  TREE: "🌳",
  BAMBOO: "🎋",
  CLOVER: "🍀",
  MUSHROOM: "🍄",
  TABLE: "🌴",
  SPATHIPHYLLUM: "🌿",
  HONG: "🌱",
  ORANGE: "🌸",
  DEFAULT: "🌱",
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

const PLANT_COLOR: Record<string, string> = {
  TABLE: "bg-emerald-100",
  SPATHIPHYLLUM: "bg-teal-100",
  HONG: "bg-lime-100",
  ORANGE: "bg-orange-100",
  DEFAULT: "bg-green-100",
};

export default function StampIcon({ stampStyle, winner = false, size = 48 }: Props) {
  const parts = stampStyle.split("_");
  const plant = parts[0];
  const job = parts[parts.length - 1];
  const plantEmoji = PLANT_EMOJI[plant] ?? PLANT_EMOJI.DEFAULT;
  const jobEmoji = JOB_EMOJI[job] ?? JOB_EMOJI.DEFAULT;
  const bgColor = PLANT_COLOR[plant] ?? PLANT_COLOR.DEFAULT;

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
