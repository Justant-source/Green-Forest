"use client";

import { useCallback, useEffect, useState } from "react";
import {
  cancelSnackRequest,
  createSnackRequest,
  getSnackCatalog,
  getSnackCategories,
} from "@/lib/api";
import { SnackCatalogCard, SnackSsgCategoryInfo } from "@/types";
import SnackCard from "@/components/requests/SnackCard";
import SnackCategoryTabs, { SnackCatalogTab } from "@/components/requests/SnackCategoryTabs";

export default function SnackRequestsPage() {
  const [items, setItems] = useState<SnackCatalogCard[]>([]);
  const [tabs, setTabs] = useState<SnackSsgCategoryInfo[]>([]);
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [requestedOnly, setRequestedOnly] = useState(false);
  const [category, setCategory] = useState<SnackCatalogTab>("ALL");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(q), 300);
    return () => clearTimeout(t);
  }, [q]);

  const loadTabs = useCallback(async () => {
    try {
      const next = await getSnackCategories();
      setTabs(next);
      setCategory((current) => {
        if (current === "ALL") {
          return "ALL";
        }
        if (current && next.some((t) => t.code === current)) {
          return current;
        }
        return "ALL";
      });
    } catch (e) {
      console.error(e);
      setTabs([]);
    }
  }, []);

  const loadCatalog = useCallback(
    async (pageNum: number, append: boolean) => {
      if (!category) {
        setItems([]);
        setHasMore(false);
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const data = await getSnackCatalog({
          q: category === "ALL" ? debouncedQ : "",
          requestedOnly: category === "ALL" && requestedOnly,
          category: category === "ALL" ? undefined : category,
          page: pageNum,
          size: 40,
        });
        setItems((prev) => (append ? [...prev, ...data.content] : data.content));
        setHasMore(!data.last);
        setPage(pageNum);
      } catch (e) {
        console.error(e);
        if (!append) setItems([]);
      } finally {
        setLoading(false);
      }
    },
    [debouncedQ, requestedOnly, category]
  );

  useEffect(() => {
    loadTabs();
  }, [loadTabs]);

  useEffect(() => {
    loadCatalog(0, false);
  }, [loadCatalog]);

  const refresh = async () => {
    await Promise.all([loadTabs(), loadCatalog(0, false)]);
  };

  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await fn();
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : "실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <section>
        <div className="mb-3">
          <SnackCategoryTabs
            value={category}
            onChange={(next) => {
              setCategory(next);
              if (next !== "ALL") {
                setQ("");
                setDebouncedQ("");
                setRequestedOnly(false);
              }
            }}
            categories={tabs}
            showAllTab
          />
        </div>
        {category === "ALL" && (
          <div className="flex items-center gap-3 mb-3">
            <input
              type="search"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="상품 이름 검색"
              className="flex-1 px-3 py-2 rounded-lg border border-gray-200 text-sm"
            />
            <label className="flex items-center gap-1.5 text-sm text-gray-600 whitespace-nowrap">
              <input
                type="checkbox"
                checked={requestedOnly}
                onChange={(e) => setRequestedOnly(e.target.checked)}
                className="rounded border-gray-300 text-forest-500"
              />
              신청된 것만
            </label>
          </div>
        )}

        {loading && items.length === 0 ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
          </div>
        ) : items.length === 0 ? (
          <div className="text-center py-20 text-gray-400">아직 간식 목록이 없습니다.</div>
        ) : (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {items.map((item) => (
                <SnackCard
                  key={item.id}
                  item={item}
                  busy={busy}
                  onRequest={(catalogItemId) => run(() => createSnackRequest(catalogItemId, 1))}
                  onCancel={(requestId) => run(() => cancelSnackRequest(requestId))}
                />
              ))}
            </div>
            {hasMore && (
              <div className="flex justify-center mt-6">
                <button
                  type="button"
                  disabled={loading}
                  onClick={() => loadCatalog(page + 1, true)}
                  className="px-6 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 disabled:opacity-50"
                >
                  {loading ? "불러오는 중..." : "더보기"}
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
