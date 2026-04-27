"use client";

interface Props {
  uploaded: number;
  lines: number;
  reward: number;
  tier: "none" | "line3" | "line5" | "blackout";
}

const TIER_LABEL: Record<Props["tier"], string> = {
  none: "0줄",
  line3: "3줄",
  line5: "5줄",
  blackout: "블랙아웃",
};

export default function BingoProgressBar({ uploaded, lines, reward, tier }: Props) {
  const pct = Math.round((uploaded / 9) * 100);
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-2">
      <div className="flex items-center justify-between text-sm text-gray-700">
        <span>업로드 진행도</span>
        <span className="font-semibold text-forest-700">{uploaded}/9</span>
      </div>
      <div className="w-full h-2 rounded-full bg-gray-100 overflow-hidden">
        <div
          className="h-full bg-forest-500 transition-all"
          style={{ width: `${pct}%` }}
        />
      </div>
      <div className="flex items-center justify-between text-xs pt-1">
        <span className="text-gray-600">
          현재 <span className="font-semibold text-gray-900">{lines}줄</span> 완성
        </span>
        <span className="text-gray-600">
          예상 보상{" "}
          <span className={`font-semibold ${reward > 0 ? "text-forest-600" : "text-gray-400"}`}>
            {reward > 0 ? `💧${reward} (${TIER_LABEL[tier]})` : "아직 없음"}
          </span>
        </span>
      </div>
    </div>
  );
}
