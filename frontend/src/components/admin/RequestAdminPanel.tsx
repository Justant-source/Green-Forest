"use client";

import { useCallback, useEffect, useState } from "react";
import {
  adminGetCatalogSyncStatus,
  adminGetGroupedSnackRequests,
  adminListSnackCatalog,
  adminListSnackHistory,
  adminListSupplyHistory,
  adminListSupplyRequests,
  adminPurchaseSnackItem,
  adminPurchaseSnackRequest,
  adminPurchaseSupplyRequest,
  adminRejectPurchasedSnackRequest,
  adminRejectSnackItem,
  adminRejectSnackRequest,
  adminRejectSupplyRequest,
  adminSetSnackVisibility,
  adminSetSnackCategoryVisibility,
  adminSyncSnackCatalog,
  toMediaUrl,
} from "@/lib/api";
import {
  AdminSnackCatalogItem,
  AdminSnackHistoryItem,
  AdminSupplyRequest,
  CatalogSyncStatusInfo,
  RequestRejectReason,
  SnackGroupedPending,
  SnackSsgCategory,
  SNACK_SSG_CATEGORIES,
} from "@/types";
import { formatKstDateTime } from "@/lib/datetime";
import SnackCategoryTabs from "@/components/requests/SnackCategoryTabs";
import SsgItemImageLink from "@/components/requests/SsgItemImageLink";

const REJECT_CHIPS: { code: RequestRejectReason; label: string }[] = [
  { code: "OUT_OF_STOCK", label: "재고부족" },
  { code: "POLICY", label: "정책제외" },
  { code: "DUPLICATE", label: "중복" },
  { code: "OTHER", label: "기타" },
];

type Subview = "snack" | "supply" | "catalog";
type QueueView = "pending" | "history";
type RejectTarget =
  | { kind: "snack"; id: number }
  | { kind: "snackAll"; catalogItemId: number }
  | { kind: "snackPurchased"; id: number }
  | { kind: "supply"; id: number }
  | null;

export default function RequestAdminPanel() {
  const [sub, setSub] = useState<Subview>("snack");
  const [queueView, setQueueView] = useState<QueueView>("pending");
  const [grouped, setGrouped] = useState<SnackGroupedPending[]>([]);
  const [snackHistory, setSnackHistory] = useState<AdminSnackHistoryItem[]>([]);
  const [supplies, setSupplies] = useState<AdminSupplyRequest[]>([]);
  const [supplyHistory, setSupplyHistory] = useState<AdminSupplyRequest[]>([]);
  const [catalog, setCatalog] = useState<AdminSnackCatalogItem[]>([]);
  const [catalogQ, setCatalogQ] = useState("");
  const [catalogCategory, setCatalogCategory] = useState<SnackSsgCategory>("SNACK");
  const [catalogPage, setCatalogPage] = useState(0);
  const [catalogHasMore, setCatalogHasMore] = useState(false);
  const [sync, setSync] = useState<CatalogSyncStatusInfo | null>(null);
  const [rejectTarget, setRejectTarget] = useState<RejectTarget>(null);
  const [rejectReason, setRejectReason] = useState<RequestRejectReason>("OUT_OF_STOCK");
  const [rejectNote, setRejectNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadGrouped = useCallback(async () => {
    setGrouped(await adminGetGroupedSnackRequests());
  }, []);

  const loadSupplies = useCallback(async () => {
    setSupplies(await adminListSupplyRequests("PENDING"));
  }, []);

  const loadSnackHistory = useCallback(async () => {
    setSnackHistory(await adminListSnackHistory());
  }, []);

  const loadSupplyHistory = useCallback(async () => {
    setSupplyHistory(await adminListSupplyHistory());
  }, []);

  const loadSync = useCallback(async () => {
    setSync(await adminGetCatalogSyncStatus());
  }, []);

  const loadCatalog = useCallback(async (pageNum: number, append: boolean, q: string) => {
    const data = await adminListSnackCatalog({
      q,
      includeUnavailable: true,
      category: catalogCategory,
      page: pageNum,
      size: 40,
    });
    setCatalog((prev) => (append ? [...prev, ...data.content] : data.content));
    setCatalogHasMore(!data.last);
    setCatalogPage(pageNum);
  }, [catalogCategory]);

  useEffect(() => {
    setError(null);
    (async () => {
      try {
        if (sub === "snack") {
          if (queueView === "pending") await loadGrouped();
          else await loadSnackHistory();
        }
        if (sub === "supply") {
          if (queueView === "pending") await loadSupplies();
          else await loadSupplyHistory();
        }
        if (sub === "catalog") await loadSync();
      } catch (e) {
        setError(e instanceof Error ? e.message : "불러오기 실패");
      }
    })();
  }, [sub, queueView, loadGrouped, loadSnackHistory, loadSupplies, loadSupplyHistory, loadSync, loadCatalog]);

  useEffect(() => {
    if (sub !== "catalog") return;
    const t = setTimeout(() => {
      loadCatalog(0, false, catalogQ).catch((e) =>
        setError(e instanceof Error ? e.message : "불러오기 실패")
      );
    }, 300);
    return () => clearTimeout(t);
  }, [catalogQ, catalogCategory, sub, loadCatalog]);

  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      if (sub === "snack") {
        await (queueView === "pending" ? loadGrouped() : loadSnackHistory());
      }
      if (sub === "supply") {
        await (queueView === "pending" ? loadSupplies() : loadSupplyHistory());
      }
      if (sub === "catalog") await Promise.all([loadSync(), loadCatalog(0, false, catalogQ)]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitReject = async () => {
    if (!rejectTarget) return;
    if (rejectReason === "OTHER" && !rejectNote.trim()) {
      setError("기타는 사유를 입력하세요.");
      return;
    }
    const note = rejectReason === "OTHER" ? rejectNote.trim() : undefined;
    await run(async () => {
      if (rejectTarget.kind === "snack") {
        await adminRejectSnackRequest(rejectTarget.id, rejectReason, note);
      } else if (rejectTarget.kind === "snackPurchased") {
        await adminRejectPurchasedSnackRequest(rejectTarget.id, rejectReason, note);
      } else if (rejectTarget.kind === "snackAll") {
        await adminRejectSnackItem(rejectTarget.catalogItemId, rejectReason, note);
      } else {
        await adminRejectSupplyRequest(rejectTarget.id, rejectReason, note);
      }
      setRejectTarget(null);
      setRejectNote("");
      setRejectReason("OUT_OF_STOCK");
    });
  };

  const tabs: { key: Subview; label: string }[] = [
    { key: "snack", label: "간식 대기" },
    { key: "supply", label: "사무용품 대기" },
    { key: "catalog", label: "카탈로그" },
  ];

  return (
    <div>
      <div className="flex gap-1 mb-4 border-b">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setSub(t.key)}
            className={`px-3 py-2 text-sm font-medium border-b-2 -mb-px ${
              sub === t.key
                ? "border-forest-500 text-forest-500"
                : "border-transparent text-gray-500"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {(sub === "snack" || sub === "supply") && (
        <div className="flex gap-2 mb-4">
          <button
            type="button"
            onClick={() => setQueueView("pending")}
            className={`px-3 py-1.5 rounded-full text-xs font-medium ${
              queueView === "pending" ? "bg-forest-500 text-white" : "bg-gray-100 text-gray-600"
            }`}
          >
            대기
          </button>
          <button
            type="button"
            onClick={() => setQueueView("history")}
            className={`px-3 py-1.5 rounded-full text-xs font-medium ${
              queueView === "history" ? "bg-forest-500 text-white" : "bg-gray-100 text-gray-600"
            }`}
          >
            이력
          </button>
        </div>
      )}

      {error && <div className="mb-3 text-sm text-red-500 whitespace-pre-wrap">{error}</div>}

      {sub === "snack" && queueView === "pending" && (
        <div className="space-y-3">
          {grouped.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm">대기 중인 간식 요청이 없습니다.</div>
          ) : (
            grouped.map((g) => (
              <div key={g.catalogItemId} className="bg-white border rounded-xl p-4">
                <div className="flex gap-3">
                  <SsgItemImageLink
                    href={g.ssgUrl}
                    className="w-16 h-16 rounded-lg bg-gray-100 overflow-hidden shrink-0 block"
                  >
                    {g.imageUrl ? (
                      <img src={toMediaUrl(g.imageUrl, "sm")} alt={g.name} className="w-full h-full object-cover" />
                    ) : null}
                  </SsgItemImageLink>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-gray-800">{g.name}</div>
                    <a
                      href={g.ssgUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-forest-500"
                    >
                      SSG에서 보기
                    </a>
                    <div className="text-sm text-gray-500 mt-1">신청 {g.requesterCount}명</div>
                  </div>
                  <div className="flex flex-col gap-1 shrink-0">
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => {
                        if (!confirm("이 상품의 대기 요청을 모두 구매 완료할까요?")) return;
                        run(() => adminPurchaseSnackItem(g.catalogItemId));
                      }}
                      className="px-3 py-1.5 text-xs font-medium bg-forest-500 text-white rounded-lg disabled:opacity-50"
                    >
                      모두 구매 완료
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setRejectTarget({ kind: "snackAll", catalogItemId: g.catalogItemId })}
                      className="px-3 py-1.5 text-xs font-medium text-red-500 border border-red-200 rounded-lg disabled:opacity-50"
                    >
                      모두 거절
                    </button>
                  </div>
                </div>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="text-left text-gray-400 border-b">
                        <th className="py-2 pr-2">닉네임</th>
                        <th className="py-2 pr-2">시각</th>
                        <th className="py-2">처리</th>
                      </tr>
                    </thead>
                    <tbody>
                      {g.lines.map((line) => (
                        <tr key={line.id} className="border-b border-gray-50">
                          <td className="py-2 pr-2">{line.nickname}</td>
                          <td className="py-2 pr-2 text-xs text-gray-500 whitespace-nowrap">
                            {formatKstDateTime(line.createdAt)}
                          </td>
                          <td className="py-2 whitespace-nowrap">
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => run(() => adminPurchaseSnackRequest(line.id))}
                              className="text-xs text-forest-500 mr-2"
                            >
                              구매 완료
                            </button>
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => setRejectTarget({ kind: "snack", id: line.id })}
                              className="text-xs text-red-500"
                            >
                              거절
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {sub === "snack" && queueView === "history" && (
        <div className="overflow-x-auto">
          {snackHistory.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm">처리된 간식 이력이 없습니다.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b">
                  <th className="py-2 pr-2">처리</th>
                  <th className="py-2 pr-2">상태</th>
                  <th className="py-2 pr-2">닉네임</th>
                  <th className="py-2 pr-2">상품</th>
                  <th className="py-2 pr-2">사유</th>
                  <th className="py-2"></th>
                </tr>
              </thead>
              <tbody>
                {snackHistory.map((row) => (
                  <tr key={row.id} className="border-b border-gray-50">
                    <td className="py-2 pr-2 whitespace-nowrap text-xs text-gray-500">
                      {formatKstDateTime(row.purchasedAt || row.rejectedAt || row.createdAt)}
                    </td>
                    <td className="py-2 pr-2 whitespace-nowrap">
                      {row.status === "PURCHASED" ? "구매 완료" : "거절"}
                    </td>
                    <td className="py-2 pr-2">{row.nickname}</td>
                    <td className="py-2 pr-2">{row.name}</td>
                    <td className="py-2 pr-2 text-xs text-gray-500">
                      {row.status === "REJECTED"
                        ? `${row.rejectReasonLabel || ""}${row.rejectNote ? ` · ${row.rejectNote}` : ""}`
                        : "—"}
                    </td>
                    <td className="py-2 whitespace-nowrap">
                      {row.status === "PURCHASED" && (
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => setRejectTarget({ kind: "snackPurchased", id: row.id })}
                          className="text-xs text-red-500"
                        >
                          거절로 변경
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {sub === "supply" && queueView === "pending" && (
        <div className="overflow-x-auto">
          {supplies.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm">대기 중인 사무용품 요청이 없습니다.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b">
                  <th className="py-2 pr-2">시각</th>
                  <th className="py-2 pr-2">닉네임</th>
                  <th className="py-2 pr-2">이름</th>
                  <th className="py-2 pr-2">수량</th>
                  <th className="py-2 pr-2">메모</th>
                  <th className="py-2">처리</th>
                </tr>
              </thead>
              <tbody>
                {supplies.map((row) => (
                  <tr key={row.id} className="border-b border-gray-50">
                    <td className="py-2 pr-2 whitespace-nowrap text-xs text-gray-500">
                      {formatKstDateTime(row.createdAt)}
                    </td>
                    <td className="py-2 pr-2">{row.nickname}</td>
                    <td className="py-2 pr-2">{row.title}</td>
                    <td className="py-2 pr-2">{row.quantity}</td>
                    <td className="py-2 pr-2 text-gray-500">{row.memo || "—"}</td>
                    <td className="py-2 whitespace-nowrap">
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => run(() => adminPurchaseSupplyRequest(row.id))}
                        className="text-xs text-forest-500 mr-2"
                      >
                        구매 완료
                      </button>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => setRejectTarget({ kind: "supply", id: row.id })}
                        className="text-xs text-red-500"
                      >
                        거절
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {sub === "supply" && queueView === "history" && (
        <div className="overflow-x-auto">
          {supplyHistory.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm">처리된 사무용품 이력이 없습니다.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b">
                  <th className="py-2 pr-2">처리</th>
                  <th className="py-2 pr-2">상태</th>
                  <th className="py-2 pr-2">닉네임</th>
                  <th className="py-2 pr-2">이름</th>
                  <th className="py-2 pr-2">수량</th>
                  <th className="py-2">사유</th>
                </tr>
              </thead>
              <tbody>
                {supplyHistory.map((row) => (
                  <tr key={row.id} className="border-b border-gray-50">
                    <td className="py-2 pr-2 whitespace-nowrap text-xs text-gray-500">
                      {formatKstDateTime(row.purchasedAt || row.rejectedAt || row.createdAt)}
                    </td>
                    <td className="py-2 pr-2 whitespace-nowrap">
                      {row.status === "PURCHASED" ? "구매 완료" : "거절"}
                    </td>
                    <td className="py-2 pr-2">{row.nickname}</td>
                    <td className="py-2 pr-2">{row.title}</td>
                    <td className="py-2 pr-2">{row.quantity}</td>
                    <td className="py-2 text-xs text-gray-500">
                      {row.status === "REJECTED"
                        ? `${row.rejectReasonLabel || ""}${row.rejectNote ? ` · ${row.rejectNote}` : ""}`
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {sub === "catalog" && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <button
              type="button"
              disabled={busy || sync?.running}
              onClick={() => run(() => adminSyncSnackCatalog())}
              className="px-4 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium disabled:opacity-50"
            >
              {sync?.running ? "갱신 중..." : "목록 갱신"}
            </button>
            <div className="text-xs text-gray-500">
              마지막 성공: {sync?.lastSuccessAt ? formatKstDateTime(sync.lastSuccessAt) : "없음"}
              {sync?.itemCount != null ? ` (${sync.itemCount}건)` : ""}
              {sync?.lastError && (
                <div className="text-red-500 mt-1">{sync.lastError}</div>
              )}
            </div>
          </div>
          <SnackCategoryTabs
            value={catalogCategory}
            onChange={(code) => {
              if (code !== "ALL") setCatalogCategory(code);
            }}
            categories={SNACK_SSG_CATEGORIES}
          />
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={busy}
              onClick={() =>
                run(() => adminSetSnackCategoryVisibility(catalogCategory, true))
              }
              className="text-xs px-3 py-1.5 rounded bg-gray-800 text-white disabled:opacity-50"
            >
              이 분류 숨김
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() =>
                run(() => adminSetSnackCategoryVisibility(catalogCategory, false))
              }
              className="text-xs px-3 py-1.5 rounded bg-gray-100 text-gray-700 disabled:opacity-50"
            >
              이 분류 표시
            </button>
          </div>
          <input
            type="search"
            value={catalogQ}
            onChange={(e) => setCatalogQ(e.target.value)}
            placeholder="카탈로그 검색"
            className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm"
          />
          <div className="space-y-2">
            {catalog.map((item) => (
              <div
                key={item.id}
                className="flex items-center gap-3 bg-white border rounded-lg px-3 py-2"
              >
                <SsgItemImageLink
                  href={item.ssgUrl}
                  className="w-12 h-12 rounded bg-gray-100 overflow-hidden shrink-0 block"
                >
                  {item.imageUrl ? (
                    <img src={toMediaUrl(item.imageUrl, "sm")} alt={item.name} className="w-full h-full object-cover" />
                  ) : null}
                </SsgItemImageLink>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium truncate">{item.name}</div>
                  <div className="text-xs text-gray-400">
                    대기 {item.pendingQty}
                    {!item.available && " · 신청불가"}
                    {item.hiddenLockedByAdmin && " · 관리자잠금"}
                  </div>
                </div>
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full ${
                    item.available ? "bg-forest-50 text-forest-600" : "bg-gray-100 text-gray-500"
                  }`}
                >
                  {item.available ? "available" : "unavailable"}
                </span>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() =>
                    run(() => adminSetSnackVisibility(item.id, !item.hiddenFromUsers))
                  }
                  className={`text-xs px-2 py-1 rounded ${
                    item.hiddenFromUsers ? "bg-gray-800 text-white" : "bg-gray-100 text-gray-600"
                  }`}
                >
                  {item.hiddenFromUsers ? "숨김" : "표시"}
                </button>
              </div>
            ))}
          </div>
          {catalogHasMore && (
            <div className="flex justify-center">
              <button
                type="button"
                disabled={busy}
                onClick={() => loadCatalog(catalogPage + 1, true, catalogQ)}
                className="px-4 py-2 bg-gray-100 rounded-lg text-sm"
              >
                더보기
              </button>
            </div>
          )}
        </div>
      )}

      {rejectTarget && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-end sm:items-center justify-center p-4">
          <div className="bg-white rounded-xl p-4 w-full max-w-sm space-y-3">
            <h3 className="font-semibold">
              {rejectTarget.kind === "snackPurchased" ? "구매 완료를 거절로 변경" : "거절 사유"}
            </h3>
            <div className="flex flex-wrap gap-2">
              {REJECT_CHIPS.map((c) => (
                <button
                  key={c.code}
                  type="button"
                  onClick={() => setRejectReason(c.code)}
                  className={`px-3 py-1.5 rounded-full text-sm ${
                    rejectReason === c.code
                      ? "bg-forest-500 text-white"
                      : "bg-gray-100 text-gray-600"
                  }`}
                >
                  {c.label}
                </button>
              ))}
            </div>
            {rejectReason === "OTHER" && (
              <textarea
                required
                maxLength={100}
                value={rejectNote}
                onChange={(e) => setRejectNote(e.target.value)}
                placeholder="사유 1–100자"
                className="w-full px-3 py-2 border rounded-lg text-sm"
                rows={3}
              />
            )}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setRejectTarget(null);
                  setRejectNote("");
                }}
                className="px-3 py-1.5 text-sm text-gray-500"
              >
                닫기
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={submitReject}
                className="px-3 py-1.5 text-sm bg-red-500 text-white rounded-lg disabled:opacity-50"
              >
                거절
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
