"use client";
import React, { useEffect, useState, useCallback } from "react";
import { GachaPrizeInfo, GachaDrawResult, AdminUser } from "@/types";
import {
  getGachaPrizes,
  drawGacha,
  getGachaQuota,
  getAdminUsers,
  adminSecretDraw,
} from "@/lib/api";
import GachaPrizeCard from "@/components/GachaPrizeCard";
import GachaDrawModal from "@/components/GachaDrawModal";
import SecretGachaModal from "@/components/SecretGachaModal";
import { useAuth } from "@/context/AuthContext";

const LS_KEY = "gf_secret_candidates";

export default function GachaPage() {
  const [prizes, setPrizes] = useState<GachaPrizeInfo[]>([]);
  const [quota, setQuota] = useState({ remainingToday: 3, limit: 3 });
  const [selectedPrize, setSelectedPrize] = useState<GachaPrizeInfo | null>(null);
  const [secretPrize, setSecretPrize] = useState<GachaPrizeInfo | null>(null);
  const [defaultSecretIds, setDefaultSecretIds] = useState<number[]>([]);
  const [members, setMembers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const { isAdmin } = useAuth();

  const loadData = useCallback(async () => {
    try {
      const reqs: Promise<any>[] = [getGachaPrizes(), getGachaQuota()];
      if (isAdmin) reqs.push(getAdminUsers());
      const results = await Promise.allSettled(reqs);
      if (results[0].status === "fulfilled") setPrizes(results[0].value);
      if (results[1].status === "fulfilled") setQuota(results[1].value);
      if (results[2] && results[2].status === "fulfilled") setMembers(results[2].value);
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // loadData를 onClose에서만 호출 — 애니메이션 중 카드 확률이 갱신되는 현상 방지
  const handleDraw = async (): Promise<GachaDrawResult> => {
    if (!selectedPrize) throw new Error("No prize selected");
    return await drawGacha(selectedPrize.id);
  };

  const handleClose = () => {
    setSelectedPrize(null);
    loadData();
  };

  const handleOpenSecretDraw = (p: GachaPrizeInfo) => {
    try {
      const stored = localStorage.getItem(LS_KEY);
      setDefaultSecretIds(stored ? JSON.parse(stored) : []);
    } catch {
      setDefaultSecretIds([]);
    }
    setSecretPrize(p);
  };

  const handleSecretClose = () => {
    setSecretPrize(null);
    loadData();
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
                isAdmin={isAdmin}
                onSecretDraw={() => handleOpenSecretDraw(p)}
              />
            ))}
          </div>
        )}
      </div>

      {selectedPrize && (
        <GachaDrawModal
          prize={selectedPrize}
          onConfirm={handleDraw}
          onClose={handleClose}
        />
      )}

      {secretPrize && (
        <SecretGachaModal
          prize={secretPrize}
          members={members}
          defaultSelectedIds={defaultSecretIds}
          onConfirm={(ids, count) => adminSecretDraw(secretPrize.id, ids, count)}
          onClose={handleSecretClose}
        />
      )}
    </div>
  );
}
