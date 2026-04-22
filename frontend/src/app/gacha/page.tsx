"use client";
import React, { useEffect, useState, useCallback } from "react";
import { GachaPrizeInfo, GachaDrawResult } from "@/types";
import {
  getGachaPrizes,
  drawGacha,
  getGachaQuota,
} from "@/lib/api";
import GachaPrizeCard from "@/components/GachaPrizeCard";
import GachaDrawModal from "@/components/GachaDrawModal";

export default function GachaPage() {
  const [prizes, setPrizes] = useState<GachaPrizeInfo[]>([]);
  const [quota, setQuota] = useState({ remainingToday: 3, limit: 3 });
  const [selectedPrize, setSelectedPrize] = useState<GachaPrizeInfo | null>(null);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    try {
      const [prizesRes, quotaRes] = await Promise.allSettled([
        getGachaPrizes(),
        getGachaQuota(),
      ]);
      if (prizesRes.status === "fulfilled") setPrizes(prizesRes.value);
      if (quotaRes.status === "fulfilled") setQuota(quotaRes.value);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleDraw = async (): Promise<GachaDrawResult> => {
    if (!selectedPrize) throw new Error("No prize selected");
    const result = await drawGacha(selectedPrize.id);
    await loadData();
    return result;
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      <div className="max-w-lg mx-auto px-4 pt-6 space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-gray-800">물방울 뽑기</h1>
          <div className="text-sm text-gray-500">
            오늘 남은 횟수:{" "}
            <strong className="text-green-600">{quota.remainingToday}</strong>/
            {quota.limit}
          </div>
        </div>

        {/* 상품 목록 */}
        {loading ? (
          <div className="text-center text-gray-400 py-8">불러오는 중...</div>
        ) : (
          <div className="space-y-3">
            {prizes.map((p) => (
              <GachaPrizeCard
                key={p.id}
                prize={p}
                onDraw={() => setSelectedPrize(p)}
                remainingDraws={quota.remainingToday}
              />
            ))}
          </div>
        )}
      </div>

      {selectedPrize && (
        <GachaDrawModal
          prize={selectedPrize}
          onConfirm={handleDraw}
          onClose={() => setSelectedPrize(null)}
        />
      )}
    </div>
  );
}
