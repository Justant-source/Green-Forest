"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { cancelSupplyRequest, createSupplyRequest, getMySupplyRequests } from "@/lib/api";
import { SupplyRequest } from "@/types";
import MyRequestList from "@/components/requests/MyRequestList";

export default function SupplyRequestsPage() {
  const [mine, setMine] = useState<SupplyRequest[]>([]);
  const [title, setTitle] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [memo, setMemo] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const loadMine = useCallback(async () => {
    try {
      setMine(await getMySupplyRequests());
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    loadMine();
  }, [loadMine]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await createSupplyRequest({
        title: title.trim(),
        quantity,
        memo: memo.trim() || null,
      });
      setTitle("");
      setQuantity(1);
      setMemo("");
      await loadMine();
    } catch (err) {
      alert(err instanceof Error ? err.message : "실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (id: number) => {
    setBusyId(id);
    try {
      await cancelSupplyRequest(id);
      await loadMine();
    } catch (err) {
      alert(err instanceof Error ? err.message : "실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <section>
        <h2 className="text-sm font-semibold text-gray-700 mb-2">내 요청</h2>
        <MyRequestList
          items={mine.map((r) => ({
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
          busyId={busyId}
          onCancel={handleCancel}
        />
      </section>

      <form onSubmit={handleSubmit} className="bg-white border border-gray-100 rounded-xl p-4 space-y-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
          <input
            required
            maxLength={80}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
            placeholder="필요한 사무용품"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">수량</label>
          <select
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
            className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
          >
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">메모 (선택)</label>
          <textarea
            maxLength={300}
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
            rows={3}
            className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full py-2.5 rounded-lg bg-forest-500 text-white text-sm font-medium disabled:opacity-50"
        >
          요청하기
        </button>
      </form>
    </div>
  );
}
