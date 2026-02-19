"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { Post, CategoryInfo } from "@/types";
import { getPosts, getCategories } from "@/lib/api";
import CategoryFilter from "./CategoryFilter";
import SortSelector from "./SortSelector";
import GridItem from "./GridItem";
import SkeletonGrid from "./SkeletonGrid";

const CACHE_KEY = "gridFeedCache";

function saveCache(data: { posts: Post[]; category: string | null; sort: string; status: string | null; page: number; hasMore: boolean; scrollY: number }) {
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(data));
  } catch {}
}

function loadCache(): { posts: Post[]; category: string | null; sort: string; status: string | null; page: number; hasMore: boolean; scrollY: number } | null {
  try {
    const raw = sessionStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export default function GridFeed() {
  const cache = useRef(loadCache());
  const [posts, setPosts] = useState<Post[]>(cache.current?.posts ?? []);
  const [category, setCategory] = useState<string | null>(cache.current?.category ?? null);
  const [sort, setSort] = useState<string>(cache.current?.sort ?? "latest");
  const [status, setStatus] = useState<string | null>(cache.current?.status ?? null);
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [page, setPage] = useState<number>(cache.current?.page ?? 0);
  const [loading, setLoading] = useState<boolean>(!cache.current);
  const [hasMore, setHasMore] = useState<boolean>(cache.current?.hasMore ?? true);
  const [restored, setRestored] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    getCategories().then(setCategories).catch(console.error);
  }, []);

  const fetchPosts = useCallback(
    async (pageNum: number, append: boolean) => {
      setLoading(true);
      try {
        const data = await getPosts(category ?? undefined, sort, pageNum, 24, status ?? undefined);
        if (append) {
          setPosts((prev) => [...prev, ...data.content]);
        } else {
          setPosts(data.content);
        }
        setHasMore(!data.last);
      } catch (error) {
        console.error("Failed to fetch posts:", error);
      } finally {
        setLoading(false);
      }
    },
    [category, sort, status]
  );

  // Save selected category for new post page
  useEffect(() => {
    if (category) {
      sessionStorage.setItem("selectedCategory", category);
    } else {
      sessionStorage.removeItem("selectedCategory");
    }
  }, [category]);

  // Reset when category or sort changes (skip if restoring from cache)
  useEffect(() => {
    if (cache.current) {
      setHasMore(cache.current.hasMore);
      cache.current = null;
      return;
    }
    setPage(0);
    setPosts([]);
    setHasMore(true);
    fetchPosts(0, false);
  }, [fetchPosts]);

  // Restore scroll position after cached posts render
  useEffect(() => {
    if (!restored && posts.length > 0) {
      const raw = sessionStorage.getItem(CACHE_KEY);
      if (raw) {
        const cached = JSON.parse(raw);
        if (cached.scrollY) {
          requestAnimationFrame(() => {
            window.scrollTo(0, cached.scrollY);
          });
        }
      }
      setRestored(true);
    }
  }, [posts, restored]);

  // Save state before navigating away
  useEffect(() => {
    const handleBeforeUnload = () => {
      saveCache({ posts, category, sort, status, page, hasMore, scrollY: window.scrollY });
    };

    const handleClick = () => {
      saveCache({ posts, category, sort, status, page, hasMore, scrollY: window.scrollY });
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    document.addEventListener("click", handleClick);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
      document.removeEventListener("click", handleClick);
    };
  }, [posts, category, sort, status, page, hasMore]);

  // Infinite scroll
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loading) {
          setPage((prev) => {
            const nextPage = prev + 1;
            fetchPosts(nextPage, true);
            return nextPage;
          });
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, loading, fetchPosts]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <CategoryFilter selected={category} onSelect={(cat) => { setCategory(cat); setStatus(null); if (!cat) setSort("latest"); }} />
      </div>

      {category && (() => {
        const catInfo = categories.find((c) => c.name === category);
        if (catInfo?.hasStatus) {
          const STATUS_OPTIONS = [
            { value: null, label: "전체" },
            { value: "REGISTERED", label: "등록" },
            { value: "ING", label: "진행중" },
            { value: "COMPLETE", label: "완료" },
          ];
          return (
            <div className="flex gap-2">
              {STATUS_OPTIONS.map((opt) => (
                <button
                  key={opt.value ?? "all"}
                  onClick={() => setStatus(opt.value)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                    status === opt.value
                      ? "bg-orange-400 text-white"
                      : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          );
        }
        return (
          <div className="flex justify-end">
            <SortSelector value={sort} onChange={setSort} />
          </div>
        );
      })()}

      {loading && posts.length === 0 ? (
        <SkeletonGrid />
      ) : (
        <>
          <div className="grid grid-cols-3 gap-0.5 sm:gap-1">
            {posts.map((post) => (
              <GridItem key={post.id} post={post} />
            ))}
          </div>

          {loading && posts.length > 0 && (
            <div className="flex justify-center py-8">
              <div className="w-8 h-8 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
            </div>
          )}
        </>
      )}

      <div ref={sentinelRef} className="h-4" />
    </div>
  );
}
