"use client";

import { PlantGrowth } from "@/types";

interface Props {
  growth: PlantGrowth;
}

const STAGES = [
  { label: "씨앗",    emoji: "🌱", minScore: 0   },
  { label: "새싹",    emoji: "🌿", minScore: 10  },
  { label: "잎2장",   emoji: "🍃", minScore: 30  },
  { label: "꽃봉오리", emoji: "🌸", minScore: 70  },
  { label: "꽃",     emoji: "🌺", minScore: 150 },
  { label: "열매",    emoji: "🍎", minScore: 300 },
];

export default function PlantLevelGuide({ growth }: Props) {
  const { stage, score } = growth;

  const stageStart = STAGES[stage].minScore;
  const stageEnd = stage < 5 ? STAGES[stage + 1].minScore : null;
  const progressPct = stageEnd
    ? Math.min(100, Math.max(0, ((score - stageStart) / (stageEnd - stageStart)) * 100))
    : 100;
  const toNext = stageEnd ? stageEnd - score : 0;

  return (
    <div className="space-y-4">
      {/* 현재 단계 + 진행 상황 */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className="text-3xl">{STAGES[stage].emoji}</span>
            <div>
              <div className="font-semibold text-gray-800 text-sm">
                {STAGES[stage].label}
                <span className="ml-1.5 text-xs text-gray-400 font-normal">Lv.{stage + 1} / {STAGES.length}</span>
              </div>
              <div className="text-xs text-gray-400">성장 점수 {score}점</div>
            </div>
          </div>
          {stageEnd ? (
            <div className="text-right">
              <div className="text-xs font-semibold text-forest-600">{toNext}점 남음</div>
              <div className="text-[10px] text-gray-400">→ {STAGES[stage + 1].label} {STAGES[stage + 1].emoji}</div>
            </div>
          ) : (
            <span className="text-xs font-semibold text-amber-500">✨ 최고 레벨</span>
          )}
        </div>

        {/* 진행 바 */}
        <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-green-400 to-forest-500 rounded-full transition-all duration-700"
            style={{ width: `${progressPct}%` }}
          />
        </div>
        <div className="flex justify-between text-[10px] text-gray-400 mt-1">
          <span>{stageStart}점</span>
          <span className="font-medium text-gray-500">{score}점</span>
          <span>{stageEnd ?? "MAX"}점</span>
        </div>
      </div>

      {/* 점수 획득 방법 */}
      <div className="bg-blue-50 rounded-xl p-3">
        <div className="text-xs font-semibold text-blue-700 mb-2">📊 점수 획득 방법</div>
        <div className="grid grid-cols-3 gap-2 text-center">
          <div className="bg-white rounded-lg py-2 px-1">
            <div className="text-base">♥</div>
            <div className="text-[11px] font-semibold text-gray-700">좋아요 받기</div>
            <div className="text-xs text-forest-600 font-bold">+1점</div>
          </div>
          <div className="bg-white rounded-lg py-2 px-1">
            <div className="text-base">💬</div>
            <div className="text-[11px] font-semibold text-gray-700">댓글 받기</div>
            <div className="text-xs text-forest-600 font-bold">+3점</div>
          </div>
          <div className="bg-white rounded-lg py-2 px-1">
            <div className="text-base">🏷️</div>
            <div className="text-[11px] font-semibold text-gray-700">칭찬 태그</div>
            <div className="text-xs text-forest-600 font-bold">+10점</div>
          </div>
        </div>
        <div className="text-[10px] text-blue-500 mt-2">
          현재: 좋아요 {growth.likesReceived}회 · 댓글 {growth.commentsReceived}회 · 칭찬태그 {growth.praisesReceived}회
        </div>
      </div>

      {/* 전체 레벨 안내 */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="text-xs font-semibold text-gray-600 mb-3">🗺️ 전체 레벨 안내</div>
        <div className="space-y-2">
          {STAGES.map((s, i) => {
            const isCurrentStage = i === stage;
            const isUnlocked = i <= stage;
            const nextMin = i < STAGES.length - 1 ? STAGES[i + 1].minScore : null;
            return (
              <div
                key={i}
                className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                  isCurrentStage
                    ? "bg-forest-50 border border-forest-200"
                    : isUnlocked
                    ? "bg-gray-50"
                    : "opacity-50"
                }`}
              >
                <span className="text-xl w-7 text-center">{s.emoji}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className={`text-sm font-semibold ${isCurrentStage ? "text-forest-700" : "text-gray-700"}`}>
                      {s.label}
                    </span>
                    {isCurrentStage && (
                      <span className="text-[10px] bg-forest-500 text-white px-1.5 py-0.5 rounded-full font-medium">현재</span>
                    )}
                    {isUnlocked && !isCurrentStage && (
                      <span className="text-[10px] text-gray-400">달성 ✓</span>
                    )}
                  </div>
                  <div className="text-[10px] text-gray-400">
                    {s.minScore}점{nextMin ? ` ~ ${nextMin - 1}점` : " 이상"}
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <div className="text-[10px] text-gray-400">대표 아이콘</div>
                  <div className="text-base">{s.emoji}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
