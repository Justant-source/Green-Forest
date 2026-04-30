"use client";

import { PlantGrowth } from "@/types";

interface Props {
  growth: PlantGrowth;
}

const STAGES = [
  { label: "씨앗",    emoji: "🌱", minScore: 0   },
  { label: "새싹",    emoji: "🌿", minScore: 20  },
  { label: "잎2장",   emoji: "🍃", minScore: 80  },
  { label: "꽃봉오리", emoji: "🌸", minScore: 200 },
  { label: "꽃",     emoji: "🌺", minScore: 400 },
  { label: "열매",    emoji: "🍎", minScore: 700 },
];

type EarnAction = {
  label: string;
  score: string;
  hint?: string;
  capKey?: string;
};

const EARN_ACTIONS: EarnAction[] = [
  { label: "출석 체크",   score: "+2점",  hint: "매일 1회" },
  { label: "연속 7일",   score: "+5점",  hint: "평생 1회" },
  { label: "연속 30일",  score: "+15점", hint: "평생 1회" },
  { label: "연속 100일", score: "+50점", hint: "평생 1회" },
  { label: "추첨 당첨",  score: "+10점", hint: "11시 추첨" },
  { label: "게시글 작성", score: "+2점",  hint: "30자↑·일3회", capKey: "POST_CREATED" },
  { label: "좋아요 받기", score: "+1점",  hint: "일 10점",   capKey: "LIKE_RECEIVED" },
  { label: "댓글 받기",  score: "+3점",  hint: "일 15점",   capKey: "COMMENT_RECEIVED" },
  { label: "칭찬 태그",  score: "+10점", hint: "일 20점",   capKey: "PRAISE_RECEIVED" },
  { label: "가챠 당첨",  score: "+5점",  hint: "당첨 시" },
  { label: "첫 활동",    score: "+5점",  hint: "각 1회" },
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
            <span className="text-xs font-semibold text-amber-500">최고 레벨</span>
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
        <div className="flex items-center justify-between mb-2">
          <div className="text-xs font-semibold text-blue-700">점수 획득 방법</div>
          <div className="text-[10px] text-blue-400">← 밀어서 더보기 →</div>
        </div>
        <div
          className="flex gap-2 overflow-x-auto snap-x snap-mandatory scrollbar-hide"
          style={{ scrollbarWidth: "none" }}
        >
          {EARN_ACTIONS.map((a, i) => {
            const cap = a.capKey && growth.todayCaps?.[a.capKey];
            return (
              <div
                key={i}
                className="snap-start flex-shrink-0 basis-[calc(33.333%-0.34rem)] min-h-[60px] bg-white rounded-lg py-2 px-1 text-center flex flex-col items-center justify-center"
              >
                <div className="text-[11px] font-semibold text-gray-700 leading-tight w-full truncate text-center" title={a.label}>
                  {a.label}
                </div>
                <div className="text-xs text-forest-600 font-bold mt-0.5">{a.score}</div>
                {cap ? (
                  <div className="text-[9px] text-gray-400 mt-0.5">
                    오늘 {cap.used}/{cap.cap}
                  </div>
                ) : a.hint ? (
                  <div className="text-[9px] text-gray-400 mt-0.5">{a.hint}</div>
                ) : null}
              </div>
            );
          })}
        </div>
        <div className="text-[10px] text-blue-500 mt-2">
          현재: 좋아요 {growth.likesReceived}회 · 댓글 {growth.commentsReceived}회 · 칭찬태그 {growth.praisesReceived}회
        </div>
      </div>

      {/* 전체 레벨 안내 */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="text-xs font-semibold text-gray-600 mb-3">전체 레벨 안내</div>
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
                      <span className="text-[10px] text-gray-400">달성</span>
                    )}
                  </div>
                  <div className="text-[10px] text-gray-400">
                    {s.minScore}점{nextMin ? ` ~ ${nextMin - 1}점` : " 이상"}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
