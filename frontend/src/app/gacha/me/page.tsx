"use client";
import React, { useEffect, useState } from "react";
import { GachaDrawRecord } from "@/types";
import { getGachaHistory } from "@/lib/api";

const DELIVERY_LABEL: Record<string, string> = {
  NONE: "낙첨",
  PENDING: "전달 대기",
  DELIVERED: "전달 완료",
};
const DELIVERY_COLOR: Record<string, string> = {
  NONE: "text-gray-400",
  PENDING: "text-orange-500",
  DELIVERED: "text-green-600",
};

export default function GachaHistoryPage() {
  const [history, setHistory] = useState<GachaDrawRecord[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  const loadMore = async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    try {
      const data = await getGachaHistory(page, 20);
      const content = data.content ?? data;
      setHistory((prev) => [...prev, ...content]);
      setHasMore(!data.last);
      setPage((p) => p + 1);
    } catch {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMore();
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      <div className="max-w-lg mx-auto px-4 pt-6">
        <h1 className="text-xl font-bold text-gray-800 mb-4">내 뽑기 기록</h1>
        <div className="space-y-2">
          {history.map((d) => (
            <div
              key={d.id}
              className={`bg-white rounded-xl p-4 shadow-sm border-l-4 ${
                d.isWinner ? "border-yellow-400" : "border-gray-200"
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <p className="font-semibold text-gray-800">{d.prizeName}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {new Date(d.createdAt).toLocaleString("ko-KR")}
                  </p>
                </div>
                <div className="text-right">
                  <span
                    className={`text-sm font-semibold ${
                      d.isWinner ? "text-yellow-600" : "text-gray-400"
                    }`}
                  >
                    {d.isWinner ? "🎊 당첨" : "꽝"}
                  </span>
                  {d.isWinner && (
                    <p
                      className={`text-xs mt-0.5 ${
                        DELIVERY_COLOR[d.deliveryStatus]
                      }`}
                    >
                      {DELIVERY_LABEL[d.deliveryStatus]}
                    </p>
                  )}
                </div>
              </div>
              <div className="flex gap-3 text-xs text-gray-400 mt-2">
                <span>💧 {d.dropsSpent}</span>
                <span>확률 {(d.winProbability * 100).toFixed(2)}%</span>
              </div>
            </div>
          ))}
        </div>
        {hasMore && (
          <button
            onClick={loadMore}
            disabled={loading}
            className="w-full mt-4 py-3 text-sm text-gray-500 hover:bg-gray-100 rounded-xl"
          >
            {loading ? "불러오는 중..." : "더 보기"}
          </button>
        )}
        {history.length === 0 && !loading && (
          <div className="text-center text-gray-400 py-12">
            뽑기 기록이 없습니다
          </div>
        )}
      </div>
    </div>
  );
}
