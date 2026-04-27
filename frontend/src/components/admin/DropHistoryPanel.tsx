"use client";

import { useEffect, useState, useCallback } from "react";
import { getToken } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

/** reasonType 그룹 정의. 관리자 필터 칩과 백엔드 쿼리 파라미터를 동시에 결정한다. */
const FILTER_GROUPS: Array<{ key: string; label: string; types: string[] }> = [
  { key: "ALL", label: "전체", types: [] },
  { key: "POST", label: "글 작성", types: ["DAILY_QUEST", "WEEKLY_QUEST", "MONTHLY_QUEST", "QUEST_COMPLETION"] },
  { key: "LIKE", label: "좋아요", types: ["LIKE_BONUS"] },
  { key: "COMMENT", label: "댓글", types: ["COMMENT_BONUS"] },
  { key: "TAG", label: "태깅", types: ["TAG_BONUS"] },
  { key: "ATTEND", label: "출석", types: ["ATTENDANCE", "ATTENDANCE_WINNER"] },
  { key: "EVENT", label: "이벤트", types: ["EVENT_REWARD"] },
  { key: "GACHA", label: "뽑기", types: ["GACHA_BET"] },
  { key: "GIFT", label: "선물", types: ["GIFT_SENT", "GIFT_RECEIVED"] },
  { key: "MANUAL", label: "수동", types: ["GM_AWARD", "GM_DEDUCT"] },
];

interface Tx {
  id: number;
  userId: number;
  userNickname: string;
  userName: string;
  amount: number;
  reasonType: string;
  reasonLabel: string;
  reasonDetail: string | null;
  relatedPostId: number | null;
  relatedQuestId: number | null;
  createdAt: string;
}

interface PageResp {
  content: Tx[];
  totalElements: number;
  totalPages: number;
  number: number;
}

async function fetchJson(url: string, init?: RequestInit) {
  const token = getToken();
  const headers: Record<string, string> = { ...(init?.headers as any) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (init?.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const res = await fetch(url, { ...init, headers, cache: "no-store" });
  if (!res.ok) {
    let msg = "요청 실패";
    try { const b = await res.json(); if (b?.message) msg = b.message; } catch {}
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

export default function DropHistoryPanel() {
  const [activeKey, setActiveKey] = useState("ALL");
  const [data, setData] = useState<PageResp | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editAmount, setEditAmount] = useState<string>("");

  const fetchPage = useCallback(async (key: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const group = FILTER_GROUPS.find((g) => g.key === key);
      const qs = new URLSearchParams();
      qs.set("page", String(p));
      qs.set("size", "30");
      if (group && group.types.length > 0) qs.set("types", group.types.join(","));
      const res = (await fetchJson(`${BASE_URL}/admin/drop-transactions?${qs.toString()}`)) as PageResp;
      setData(res);
    } catch (e: any) {
      setError(e?.message ?? "불러오기 실패");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPage(activeKey, page);
  }, [activeKey, page, fetchPage]);

  const onSelectKey = (key: string) => {
    setActiveKey(key);
    setPage(0);
  };

  const handleRevoke = async (tx: Tx) => {
    const desc = tx.amount >= 0 ? `지급된 +${tx.amount}` : `차감된 ${tx.amount}`;
    if (!confirm(`${tx.userNickname}님의 '${tx.reasonLabel}' (${desc} 💧) 거래를 회수하시겠습니까?`)) return;
    try {
      await fetchJson(`${BASE_URL}/admin/drop-transactions/${tx.id}/revoke`, { method: "POST" });
      fetchPage(activeKey, page);
    } catch (e: any) {
      alert(e?.message ?? "회수 실패");
    }
  };

  const startEdit = (tx: Tx) => {
    setEditingId(tx.id);
    setEditAmount(String(tx.amount));
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditAmount("");
  };

  const saveEdit = async (tx: Tx) => {
    const n = Number(editAmount);
    if (!Number.isFinite(n)) return alert("숫자를 입력하세요.");
    if (n === tx.amount) {
      cancelEdit();
      return;
    }
    try {
      await fetchJson(`${BASE_URL}/admin/drop-transactions/${tx.id}`, {
        method: "PATCH",
        body: JSON.stringify({ amount: n }),
      });
      cancelEdit();
      fetchPage(activeKey, page);
    } catch (e: any) {
      alert(e?.message ?? "수정 실패");
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-800">지급 이력</h3>
        {data && (
          <span className="text-xs text-gray-500">
            전체 {data.totalElements.toLocaleString()}건
          </span>
        )}
      </div>

      <div className="flex flex-wrap gap-1.5">
        {FILTER_GROUPS.map((g) => (
          <button
            key={g.key}
            onClick={() => onSelectKey(g.key)}
            className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
              activeKey === g.key
                ? "bg-forest-500 border-forest-500 text-white"
                : "bg-white border-gray-200 text-gray-700 hover:border-gray-300"
            }`}
          >
            {g.label}
          </button>
        ))}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
          {error}
        </div>
      )}

      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
        {loading && !data ? (
          <div className="flex justify-center py-10">
            <div className="w-8 h-8 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="text-center text-gray-500 py-10 text-sm">이력이 없습니다.</div>
        ) : (
          <ul className="divide-y">
            {data.content.map((tx) => {
              const positive = tx.amount >= 0;
              const isEditing = editingId === tx.id;
              return (
                <li key={tx.id} className="px-3 py-2.5 flex items-center gap-2 text-sm">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium text-gray-900 truncate">{tx.userNickname}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                        {tx.reasonLabel || tx.reasonType}
                      </span>
                      <span className="text-[11px] text-gray-400">
                        {tx.createdAt?.replace("T", " ").substring(0, 16)}
                      </span>
                    </div>
                    {tx.reasonDetail && (
                      <div className="text-xs text-gray-500 truncate mt-0.5" title={tx.reasonDetail}>
                        {tx.reasonDetail}
                      </div>
                    )}
                  </div>
                  <div className="flex items-center gap-1.5">
                    {isEditing ? (
                      <>
                        <input
                          type="number"
                          value={editAmount}
                          onChange={(e) => setEditAmount(e.target.value)}
                          className="w-20 px-2 py-1 border border-gray-300 rounded text-sm text-right"
                          autoFocus
                        />
                        <button
                          onClick={() => saveEdit(tx)}
                          className="px-2 py-1 rounded bg-forest-500 hover:bg-forest-600 text-white text-xs"
                        >
                          저장
                        </button>
                        <button
                          onClick={cancelEdit}
                          className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs"
                        >
                          취소
                        </button>
                      </>
                    ) : (
                      <>
                        <div className={`font-semibold w-16 text-right ${positive ? "text-forest-700" : "text-red-600"}`}>
                          {positive ? "+" : ""}
                          {tx.amount}
                        </div>
                        <button
                          onClick={() => startEdit(tx)}
                          className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs"
                          title="금액 수정"
                        >
                          수정
                        </button>
                        <button
                          onClick={() => handleRevoke(tx)}
                          className="px-2 py-1 rounded bg-red-50 hover:bg-red-100 text-red-600 text-xs"
                          title="거래 회수"
                        >
                          회수
                        </button>
                      </>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 text-sm">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
          >
            이전
          </button>
          <span className="text-gray-600">
            {page + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
