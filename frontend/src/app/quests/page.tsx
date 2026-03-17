"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getQuests } from "@/lib/api";
import { Quest } from "@/types";
import { useAuth } from "@/context/AuthContext";

export default function QuestsPage() {
  const router = useRouter();
  const { isLoggedIn, authLoaded } = useAuth();
  const [quests, setQuests] = useState<Quest[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }
    getQuests()
      .then(setQuests)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [authLoaded, isLoggedIn, router]);

  if (!isLoggedIn) return null;

  const now = new Date();
  const activeQuests = quests.filter((q) => q.active && new Date(q.endDate) >= now);
  const expiredQuests = quests.filter((q) => !q.active || new Date(q.endDate) < now);

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">퀘스트</h1>

      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
        </div>
      ) : (
        <div className="space-y-8">
          {activeQuests.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold mb-3 text-forest-600">진행중인 퀘스트</h2>
              <div className="space-y-3">
                {activeQuests.map((quest) => (
                  <QuestCard key={quest.id} quest={quest} />
                ))}
              </div>
            </section>
          )}

          {expiredQuests.length > 0 && (
            <section>
              <h2 className="text-lg font-semibold mb-3 text-gray-400">종료된 퀘스트</h2>
              <div className="space-y-3 opacity-60">
                {expiredQuests.map((quest) => (
                  <QuestCard key={quest.id} quest={quest} />
                ))}
              </div>
            </section>
          )}

          {quests.length === 0 && (
            <div className="text-center py-20 text-gray-400">
              진행 중인 퀘스트가 없습니다.
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function QuestCard({ quest }: { quest: Quest }) {
  const daysLeft = Math.ceil((new Date(quest.endDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24));

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-semibold text-gray-900">{quest.title}</h3>
            {quest.voteType && (
              <span className="px-2 py-0.5 bg-accent/10 text-accent-dark text-[10px] font-medium rounded-full">
                투표
              </span>
            )}
          </div>
          <p className="text-sm text-gray-500 mb-3">{quest.description}</p>
          <div className="flex items-center gap-4 text-xs text-gray-400">
            <span>{quest.startDate} ~ {quest.endDate}</span>
            <span>대상: {quest.targetType}</span>
            {quest.completionCount > 0 && (
              <span>{quest.completionCount}명 완료</span>
            )}
          </div>
        </div>
        <div className="text-right ml-4">
          <div className="text-lg font-bold text-accent">+{quest.rewardDrops}</div>
          <div className="text-[10px] text-gray-400">물방울</div>
          {daysLeft > 0 && quest.active && (
            <div className="text-xs text-forest-500 font-medium mt-1">D-{daysLeft}</div>
          )}
        </div>
      </div>
    </div>
  );
}
