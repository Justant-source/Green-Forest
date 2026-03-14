"use client";

import { useState, useEffect } from "react";
import { getLeaderboard, getPartyMembers } from "@/lib/api";
import { LeaderboardEntry, PartyMember } from "@/types";

export default function RankingPage() {
  const [period, setPeriod] = useState<string>("all_time");
  const [rankings, setRankings] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedParty, setExpandedParty] = useState<number | null>(null);
  const [members, setMembers] = useState<PartyMember[]>([]);
  const [membersLoading, setMembersLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    getLeaderboard(period)
      .then(setRankings)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [period]);

  const handleExpandParty = async (partyId: number) => {
    if (expandedParty === partyId) {
      setExpandedParty(null);
      setMembers([]);
      return;
    }
    setExpandedParty(partyId);
    setMembersLoading(true);
    try {
      const data = await getPartyMembers(partyId, period);
      setMembers(data);
    } catch (error) {
      console.error("Failed to fetch members:", error);
    } finally {
      setMembersLoading(false);
    }
  };

  const medalColors = ["text-yellow-500", "text-gray-400", "text-amber-600"];

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">파티 랭킹</h1>

      <div className="flex gap-2 mb-6">
        {[
          { value: "all_time", label: "전체" },
          { value: "monthly", label: "이번 달" },
        ].map((opt) => (
          <button
            key={opt.value}
            onClick={() => setPeriod(opt.value)}
            className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
              period === opt.value
                ? "bg-forest-500 text-white"
                : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
        </div>
      ) : rankings.length === 0 ? (
        <div className="text-center py-20 text-gray-400">랭킹 데이터가 없습니다.</div>
      ) : (
        <div className="space-y-3">
          {rankings.map((entry, i) => (
            <div key={entry.partyId}>
              <button
                onClick={() => handleExpandParty(entry.partyId)}
                className="w-full flex items-center gap-4 px-5 py-4 bg-white rounded-xl border border-gray-200 hover:border-forest-300 transition-colors text-left"
              >
                <span className={`text-2xl font-bold w-8 text-center ${i < 3 ? medalColors[i] : "text-gray-400"}`}>
                  {i + 1}
                </span>
                <div className="flex-1">
                  <div className="font-semibold text-gray-900">{entry.partyName}</div>
                  <div className="text-xs text-gray-400">{entry.memberCount}명</div>
                </div>
                <div className="text-right">
                  <div className="font-bold text-forest-500 text-lg">{entry.totalDrops.toLocaleString()}</div>
                  <div className="text-xs text-gray-400">물방울</div>
                </div>
                <svg
                  className={`w-4 h-4 text-gray-400 transition-transform ${expandedParty === entry.partyId ? "rotate-180" : ""}`}
                  fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {expandedParty === entry.partyId && (
                <div className="ml-12 mt-2 space-y-1">
                  {membersLoading ? (
                    <div className="text-sm text-gray-400 py-2">불러오는 중...</div>
                  ) : members.length === 0 ? (
                    <div className="text-sm text-gray-400 py-2">멤버가 없습니다.</div>
                  ) : (
                    members.map((member, mi) => (
                      <div key={member.userId} className="flex items-center gap-3 px-4 py-2 bg-gray-50 rounded-lg">
                        <span className="text-sm text-gray-400 w-6 text-center">{mi + 1}</span>
                        <span className="flex-1 text-sm font-medium text-gray-700">{member.nickname}</span>
                        <span className="text-sm text-forest-500 font-medium">{member.totalDrops.toLocaleString()}</span>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
