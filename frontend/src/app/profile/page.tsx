"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getMyPosts, getMyBookmarks } from "@/lib/api";
import { Post, PageResponse } from "@/types";
import GridItem from "@/components/GridItem";

type Tab = "posts" | "bookmarks";

export default function ProfilePage() {
  const { nickname, isLoggedIn, handleLogout } = useAuth();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<Tab>("posts");
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login");
    }
  }, [isLoggedIn, router]);

  const fetchData = useCallback(async (tab: Tab, pageNum: number, append: boolean) => {
    setLoading(true);
    try {
      const fetcher = tab === "posts" ? getMyPosts : getMyBookmarks;
      const data: PageResponse<Post> = await fetcher(pageNum, 12);
      setPosts((prev) => append ? [...prev, ...data.content] : data.content);
      setHasMore(!data.last);
    } catch (error) {
      console.error("Failed to fetch:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isLoggedIn) {
      setPage(0);
      fetchData(activeTab, 0, false);
    }
  }, [activeTab, isLoggedIn, fetchData]);

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchData(activeTab, nextPage, true);
  };

  const handleBookmarkChange = (postId: number, bookmarked: boolean) => {
    if (activeTab === "bookmarks" && !bookmarked) {
      setPosts((prev) => prev.filter((p) => p.id !== postId));
    }
  };

  if (!isLoggedIn) return null;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">{nickname}</h1>
          <p className="text-gray-500 text-sm mt-1">마이페이지</p>
        </div>
        <button
          onClick={() => {
            handleLogout();
            router.push("/");
          }}
          className="text-sm text-gray-400 hover:text-gray-600 px-4 py-2 rounded-lg hover:bg-gray-100 transition-colors"
        >
          로그아웃
        </button>
      </div>

      <div className="flex gap-1 mb-6 border-b">
        <button
          onClick={() => setActiveTab("posts")}
          className={`px-6 py-3 text-sm font-medium transition-colors border-b-2 -mb-px ${
            activeTab === "posts"
              ? "border-orange-600 text-orange-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          내가 쓴 글
        </button>
        <button
          onClick={() => setActiveTab("bookmarks")}
          className={`px-6 py-3 text-sm font-medium transition-colors border-b-2 -mb-px ${
            activeTab === "bookmarks"
              ? "border-orange-600 text-orange-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          북마크
        </button>
      </div>

      {loading && posts.length === 0 ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
        </div>
      ) : posts.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          {activeTab === "posts" ? "작성한 글이 없습니다." : "북마크한 글이 없습니다."}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-3 gap-0.5 sm:gap-1">
            {posts.map((post) => (
              <GridItem key={post.id} post={post} onBookmarkChange={handleBookmarkChange} />
            ))}
          </div>
          {hasMore && (
            <div className="flex justify-center mt-8">
              <button
                onClick={loadMore}
                disabled={loading}
                className="px-6 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors disabled:opacity-50"
              >
                {loading ? "불러오는 중..." : "더보기"}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
