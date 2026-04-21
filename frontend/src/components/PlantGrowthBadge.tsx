"use client";
import React from "react";
import { PlantGrowth } from "@/types";

interface Props {
  growth: PlantGrowth | null;
  showDetails?: boolean;
}

const STAGE_EMOJI = ["🌱", "🌿", "🍃", "🌸", "🌺", "🍎"];

export default function PlantGrowthBadge({
  growth,
  showDetails = false,
}: Props) {
  if (!growth) return null;

  const emoji = STAGE_EMOJI[growth.stage] ?? STAGE_EMOJI[0];
  const progressPct =
    growth.nextStageScore > 0
      ? Math.min(100, (growth.score / growth.nextStageScore) * 100)
      : 100;

  return (
    <div className="inline-flex items-center gap-1.5">
      <span className="text-xl">{emoji}</span>
      <div>
        <span className="text-xs font-semibold text-gray-700">
          {growth.stageLabel}
        </span>
        {showDetails && (
          <div className="w-24 h-1.5 bg-gray-200 rounded-full mt-0.5 overflow-hidden">
            <div
              className="h-full bg-green-400 rounded-full transition-all duration-500"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        )}
      </div>
    </div>
  );
}
