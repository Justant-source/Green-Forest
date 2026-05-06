"use client";

import { useEffect, useState } from "react";
import { Survey, SurveyOption } from "@/types";
import { getSurveyByPost, voteOnSurvey, addSurveyOption, closeSurvey, getSurveyVotes } from "@/lib/api";
import { toMediaUrl } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { SurveyVoteDetail } from "@/types";

export default function SurveyView({ postId, onImageSelect }: { postId: number; onImageSelect?: (url: string) => void }) {
  const { isLoggedIn, isAdmin, authLoaded } = useAuth();
  const [survey, setSurvey] = useState<Survey | null>(null);
  const [loading, setLoading] = useState(true);
  const [newOptionText, setNewOptionText] = useState("");
  const [voteDetails, setVoteDetails] = useState<SurveyVoteDetail[] | null>(null);
  const [showVotes, setShowVotes] = useState(false);
  const [loadingVotes, setLoadingVotes] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setSurvey(await getSurveyByPost(postId));
    } catch {
      setSurvey(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [postId]);

  if (loading)
    return <div className="py-8 text-center text-gray-400">설문 불러오는 중...</div>;
  if (!survey)
    return <div className="py-8 text-center text-gray-400">설문을 찾을 수 없습니다.</div>;

  const handleVote = async (optionId: number, imageUrl?: string | null) => {
    if (imageUrl) onImageSelect?.(imageUrl);
    if (survey.closed || !isLoggedIn) return;
    try {
      await voteOnSurvey(survey.id, optionId);
      setVoteDetails(null); // 현황 캐시 무효화
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "투표 실패");
    }
  };

  const handleClose = async () => {
    if (!survey || !confirm("투표를 즉시 종료하시겠습니까?")) return;
    try {
      await closeSurvey(survey.id);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "종료 실패");
    }
  };

  const handleToggleVotes = async () => {
    if (showVotes) { setShowVotes(false); return; }
    if (voteDetails) { setShowVotes(true); return; }
    if (!survey) return;
    setLoadingVotes(true);
    try {
      setVoteDetails(await getSurveyVotes(survey.id));
      setShowVotes(true);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "불러오기 실패");
    } finally {
      setLoadingVotes(false);
    }
  };

  const handleAddOption = async () => {
    if (!newOptionText.trim()) return;
    try {
      await addSurveyOption(survey.id, newOptionText.trim());
      setNewOptionText("");
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "옵션 추가 실패");
    }
  };

  const closesAt = new Date(survey.closesAt.endsWith("Z") ? survey.closesAt : survey.closesAt + "Z");

  return (
    <div className="space-y-3">
      {/* 메타 정보 */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <div className="flex gap-2 flex-wrap">
          {survey.allowMultiSelect && <Badge>복수 선택</Badge>}
          {survey.anonymous && <Badge>익명</Badge>}
          {survey.notice && <Badge color="amber">공지</Badge>}
        </div>
        <div className="flex items-center gap-2">
          {authLoaded && isAdmin && !survey.closed && (
            <button
              onClick={handleClose}
              className="px-2 py-0.5 rounded text-[10px] font-semibold bg-red-50 text-red-600 hover:bg-red-100 transition-colors"
            >
              투표 종료
            </button>
          )}
          <span>
            {survey.closed ? "종료됨" : `~ ${closesAt.toLocaleString("ko-KR")}`}
          </span>
        </div>
      </div>

      {/* 관리자 — 투표 현황 */}
      {authLoaded && isAdmin && (
        <div>
          <button
            onClick={handleToggleVotes}
            disabled={loadingVotes}
            className="text-xs font-medium text-indigo-600 hover:text-indigo-800 underline underline-offset-2 disabled:opacity-50"
          >
            {loadingVotes ? "불러오는 중..." : showVotes ? "투표 현황 닫기" : "투표 현황 보기"}
          </button>

          {showVotes && voteDetails && (
            <div className="mt-2 border border-indigo-100 rounded-lg overflow-hidden">
              {voteDetails.map((d, i) => (
                <div key={d.optionId} className={`px-3 py-2 ${i !== 0 ? "border-t border-indigo-100" : ""}`}>
                  <div className="flex items-center gap-2 mb-1">
                    {d.imageUrl && (
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img src={toMediaUrl(d.imageUrl, "sm")} alt="" className="w-6 h-6 object-cover rounded flex-shrink-0" />
                    )}
                    <span className="text-xs font-semibold text-gray-700 truncate">
                      {d.text || "(이미지 옵션)"}
                    </span>
                    <span className="ml-auto text-xs text-gray-400 shrink-0">{d.voteCount}표</span>
                  </div>
                  {d.voters.length === 0 ? (
                    <p className="text-[11px] text-gray-400 pl-1">투표 없음</p>
                  ) : (
                    <div className="flex flex-wrap gap-1 pl-1">
                      {d.voters.map((v) => (
                        <span key={v.userId} className="inline-block px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700 text-[11px]">
                          {v.name || v.nickname}
                          <span className="text-indigo-400 ml-0.5">({v.nickname})</span>
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 옵션 목록 */}
      <div className="space-y-2">
        {survey.options.map((o) => (
          <OptionRow
            key={o.id}
            option={o}
            totalVotes={survey.totalVotes}
            closed={survey.closed}
            isLoggedIn={isLoggedIn}
            onVote={() => handleVote(o.id, o.imageUrl)}
            onImageClick={o.imageUrl ? () => onImageSelect?.(o.imageUrl!) : undefined}
          />
        ))}
      </div>

      {/* 사용자 옵션 추가 */}
      {survey.allowOptionAddByUser && !survey.closed && isLoggedIn && (
        <div className="flex gap-2 pt-2">
          <input
            type="text"
            value={newOptionText}
            onChange={(e) => setNewOptionText(e.target.value)}
            maxLength={50}
            placeholder="옵션 추가하기 (텍스트만, 50자 이내)"
            onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddOption(); } }}
            className="flex-1 px-3 py-1.5 border border-gray-300 rounded-md text-sm"
          />
          <button
            onClick={handleAddOption}
            className="px-4 py-1.5 bg-forest-500 text-white text-sm rounded-md"
          >
            추가
          </button>
        </div>
      )}

      <div className="text-xs text-gray-400 text-right">총 {survey.totalVotes}표</div>
    </div>
  );
}

function OptionRow({
  option,
  totalVotes,
  closed,
  isLoggedIn,
  onVote,
  onImageClick,
}: {
  option: SurveyOption;
  totalVotes: number;
  closed: boolean;
  isLoggedIn: boolean;
  onVote: () => void;
  onImageClick?: () => void;
}) {
  const pct = totalVotes > 0 ? (option.voteCount / totalVotes) * 100 : 0;
  const canVote = !closed && isLoggedIn;

  return (
    <button
      onClick={canVote ? onVote : undefined}
      disabled={!canVote}
      className={`w-full text-left rounded-lg border-2 transition-all
        ${option.voted ? "border-forest-500 bg-forest-50" : "border-gray-200 bg-white"}
        ${canVote ? "hover:border-forest-300 cursor-pointer" : "cursor-default"}`}
    >
      <div className="relative px-3 py-2.5 overflow-hidden">
        <div
          className="absolute inset-y-0 left-0 bg-forest-100 opacity-40 transition-all"
          style={{ width: `${pct}%` }}
        />
        <div className="relative flex items-center gap-3">
          {option.imageUrl && (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img
              src={toMediaUrl(option.imageUrl, "sm")}
              alt=""
              onClick={(e) => { if (onImageClick) { e.stopPropagation(); onImageClick(); } }}
              className={`w-12 h-12 object-cover rounded flex-shrink-0 ${onImageClick ? "cursor-zoom-in ring-2 ring-transparent hover:ring-forest-400 transition-all" : ""}`}
            />
          )}
          <div className="flex-1 min-w-0">
            <div className="text-sm font-medium text-gray-800 truncate">
              {option.text || "(이미지 옵션)"}
              {option.addedByUser && (
                <span className="ml-1 text-[10px] text-gray-400">추가됨</span>
              )}
            </div>
          </div>
          <div className="text-xs font-semibold text-gray-600 shrink-0">
            {option.voteCount}표 · {pct.toFixed(0)}%
          </div>
        </div>
      </div>
    </button>
  );
}

function Badge({
  children,
  color = "indigo",
}: {
  children: React.ReactNode;
  color?: "indigo" | "amber";
}) {
  const colorMap: Record<string, string> = {
    indigo: "bg-indigo-50 text-indigo-700",
    amber: "bg-amber-50 text-amber-700",
  };
  return (
    <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${colorMap[color]}`}>
      {children}
    </span>
  );
}
