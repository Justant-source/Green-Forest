"use client";

import { useCallback, useEffect, useState } from "react";
import { getMySnackRequests, getMySupplyRequests } from "@/lib/api";
import { SnackRequest, SupplyRequest } from "@/types";
import MyRequestList from "@/components/requests/MyRequestList";

const POLL_MS = 8000;

export default function MyRequestsPage() {
  const [snacks, setSnacks] = useState<SnackRequest[]>([]);
  const [supplies, setSupplies] = useState<SupplyRequest[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (silent: boolean) => {
    if (!silent) setLoading(true);
    try {
      const [snackList, supplyList] = await Promise.all([
        getMySnackRequests().catch(() => [] as SnackRequest[]),
        getMySupplyRequests().catch(() => [] as SupplyRequest[]),
      ]);
      setSnacks(snackList);
      setSupplies(supplyList);
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(false);
    const onFocus = () => {
      if (document.visibilityState === "visible") load(true);
    };
    const poll = setInterval(() => {
      if (document.visibilityState === "visible") load(true);
    }, POLL_MS);
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onFocus);
    return () => {
      clearInterval(poll);
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onFocus);
    };
  }, [load]);

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <section>
        <h2 className="text-sm font-semibold text-gray-700 mb-2">간식</h2>
        <MyRequestList
          items={snacks.map((r) => ({
            id: r.id,
            title: r.name,
            imageUrl: r.imageUrl,
            ssgUrl: r.ssgUrl,
            quantity: r.quantity,
            status: r.status,
            rejectReasonLabel: r.rejectReasonLabel,
            rejectNote: r.rejectNote,
            createdAt: r.createdAt,
            purchasedAt: r.purchasedAt,
            rejectedAt: r.rejectedAt,
          }))}
          emptyText="요청한 간식이 없습니다."
          showQuantity={false}
        />
      </section>
      <section>
        <h2 className="text-sm font-semibold text-gray-700 mb-2">사무용품</h2>
        <MyRequestList
          items={supplies.map((r) => ({
            id: r.id,
            title: r.title,
            quantity: r.quantity,
            status: r.status,
            rejectReasonLabel: r.rejectReasonLabel,
            rejectNote: r.rejectNote,
            createdAt: r.createdAt,
            purchasedAt: r.purchasedAt,
            rejectedAt: r.rejectedAt,
          }))}
          emptyText="요청한 사무용품이 없습니다."
        />
      </section>
    </div>
  );
}
