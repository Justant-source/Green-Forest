"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Post, CategoryInfo } from "@/types";
import { getPost, getCategories, toggleLike, getLikeStatus, toggleBookmark, getBookmarkStatus, updatePostStatus, deletePost, startConversation, IMAGE_BASE_URL } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import CommentSection from "./CommentSection";
import PostContent from "./PostContent";

interface PostDetailProps {
  postId: number;
}

const colorClassMap: Record<string, string> = {
  yellow: "bg-yellow-100 text-yellow-800",
  blue: "bg-blue-100 text-blue-800",
  orange: "bg-orange-100 text-orange-800",
  purple: "bg-purple-100 text-purple-800",
  green: "bg-green-100 text-green-800",
  red: "bg-red-100 text-red-800",
  pink: "bg-pink-100 text-pink-800",
  indigo: "bg-indigo-100 text-indigo-800",
  gray: "bg-gray-100 text-gray-800",
};

export default function PostDetail({ postId }: PostDetailProps) {
  const router = useRouter();
  const [post, setPost] = useState<Post | null>(null);
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [bookmarked, setBookmarked] = useState(false);
  const [carouselIndex, setCarouselIndex] = useState(0);
  const { isLoggedIn, nickname } = useAuth();
  const touchStartX = useRef<number | null>(null);

  useEffect(() => {
    getCategories()
      .then((cats) => { if (cats.length > 0) setCategories(cats); })
      .catch(console.error);
    getPost(postId)
      .then((postData) => {
        setPost(postData);
        if (isLoggedIn) {
          getLikeStatus(postId)
            .then((res) => setPost((prev) => prev ? { ...prev, liked: res.liked } : prev))
            .catch(console.error);
          getBookmarkStatus(postId)
            .then((res) => setBookmarked(res.bookmarked))
            .catch(console.error);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [postId, isLoggedIn]);

  const handleLike = async () => {
    if (!post || !isLoggedIn) return;
    try {
      const updated = await toggleLike(post.id);
      setPost(updated);
    } catch (error) {
      console.error("Failed to toggle like:", error);
    }
  };

  const handleBookmark = async () => {
    try {
      const res = await toggleBookmark(postId);
      setBookmarked(res.bookmarked);
    } catch (error) {
      console.error("Failed to toggle bookmark:", error);
    }
  };

  const getCategoryInfo = (name: string) => {
    return categories.find((c) => c.name === name);
  };

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
      </div>
    );
  }

  if (!post) {
    return (
      <div className="text-center py-20 text-gray-500">
        게시글을 찾을 수 없습니다.
      </div>
    );
  }

  const catInfo = getCategoryInfo(post.category);
  const colorClass = catInfo ? (colorClassMap[catInfo.color] || "bg-gray-100 text-gray-800") : "bg-gray-100 text-gray-800";

  const STATUS_OPTIONS = [
    { value: "REGISTERED", label: "등록" },
    { value: "ING", label: "진행중" },
    { value: "COMPLETE", label: "완료" },
  ];
  const statusLabelMap: Record<string, string> = { REGISTERED: "등록", ING: "진행중", COMPLETE: "완료" };
  const statusColorMap: Record<string, string> = {
    REGISTERED: "bg-blue-100 text-blue-700",
    ING: "bg-yellow-100 text-yellow-700",
    COMPLETE: "bg-green-100 text-green-700",
  };

  const handleStatusChange = async (newStatus: string) => {
    try {
      const updated = await updatePostStatus(post.id, newStatus);
      setPost(updated);
    } catch (error) {
      console.error("Failed to update status:", error);
      alert("상태 변경에 실패했습니다.");
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span
              className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${colorClass}`}
            >
              {catInfo?.label || post.category}
            </span>
            {post.status && (
              isLoggedIn && nickname === post.authorNickname ? (
                <select
                  value={post.status}
                  onChange={(e) => handleStatusChange(e.target.value)}
                  className={`px-2 py-1 rounded-full text-xs font-medium border-0 cursor-pointer ${statusColorMap[post.status] || "bg-gray-100 text-gray-700"}`}
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>{s.label}</option>
                  ))}
                </select>
              ) : (
                <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${statusColorMap[post.status] || "bg-gray-100 text-gray-700"}`}>
                  {statusLabelMap[post.status] || post.status}
                </span>
              )
            )}
          </div>
          {isLoggedIn && nickname === post.authorNickname && (
            <div className="flex gap-2">
              <Link
                href={`/posts/${post.id}/edit`}
                className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
              >
                수정
              </Link>
              <button
                onClick={async () => {
                  if (!confirm("정말 삭제하시겠습니까?")) return;
                  try {
                    await deletePost(post.id);
                    router.push("/");
                  } catch {
                    alert("삭제에 실패했습니다.");
                  }
                }}
                className="px-4 py-1.5 border border-red-300 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 transition-colors"
              >
                삭제
              </button>
            </div>
          )}
        </div>
        <h1 className="text-2xl font-bold mt-3">{post.title}</h1>
        <div className="flex gap-4 text-sm text-gray-500 mt-2">
          {post.authorNickname && (
            isLoggedIn && nickname !== post.authorNickname ? (
              <button
                onClick={async () => {
                  try {
                    const res = await startConversation(post.authorNickname!);
                    router.push(`/conversations/${res.conversationId}`);
                  } catch {
                    alert("대화를 시작할 수 없습니다.");
                  }
                }}
                className="text-gray-700 font-medium hover:text-orange-600 transition-colors cursor-pointer"
              >
                {post.authorNickname}
              </button>
            ) : (
              <span className="text-gray-700 font-medium">{post.authorNickname}</span>
            )
          )}
          <span>조회 {post.viewCount}</span>
          <span>좋아요 {post.likeCount}</span>
          <span>{new Date(post.createdAt.endsWith("Z") ? post.createdAt : post.createdAt + "Z").toLocaleDateString("ko-KR")}</span>
        </div>
      </div>

      {(() => {
        const urls = post.imageUrls && post.imageUrls.length > 0
          ? post.imageUrls
          : post.imageUrl ? [post.imageUrl] : [];
        if (urls.length === 0) return null;
        return (
          <div
            className="relative w-full rounded-xl overflow-hidden"
            onTouchStart={(e) => { touchStartX.current = e.touches[0].clientX; }}
            onTouchEnd={(e) => {
              if (touchStartX.current === null) return;
              const diff = touchStartX.current - e.changedTouches[0].clientX;
              if (Math.abs(diff) > 50) {
                if (diff > 0) setCarouselIndex((prev) => (prev + 1) % urls.length);
                else setCarouselIndex((prev) => (prev - 1 + urls.length) % urls.length);
              }
              touchStartX.current = null;
            }}
          >
            <img
              src={`${IMAGE_BASE_URL}${urls[carouselIndex]}`}
              alt={`${post.title} ${carouselIndex + 1}`}
              className="w-full rounded-xl"
            />
            {urls.length > 1 && (
              <>
                <button
                  onClick={() => setCarouselIndex((prev) => (prev - 1 + urls.length) % urls.length)}
                  className="absolute left-2 top-1/2 -translate-y-1/2 w-9 h-9 bg-black/40 hover:bg-black/60 text-white rounded-full hidden md:flex items-center justify-center text-lg"
                >
                  &lt;
                </button>
                <button
                  onClick={() => setCarouselIndex((prev) => (prev + 1) % urls.length)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 bg-black/40 hover:bg-black/60 text-white rounded-full hidden md:flex items-center justify-center text-lg"
                >
                  &gt;
                </button>
                <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
                  {urls.map((_, i) => (
                    <button
                      key={i}
                      onClick={() => setCarouselIndex(i)}
                      className={`w-2.5 h-2.5 rounded-full transition-colors ${
                        i === carouselIndex ? "bg-white" : "bg-white/50"
                      }`}
                    />
                  ))}
                </div>
              </>
            )}
          </div>
        );
      })()}

      <PostContent content={post.content} />

      <div className="flex items-center gap-4 pt-4 border-t">
        {isLoggedIn ? (
          <button
            onClick={handleLike}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors font-medium text-sm ${
              post.liked
                ? "bg-rose-50 text-rose-600 hover:bg-rose-100"
                : "bg-gray-50 text-gray-500 hover:bg-gray-100"
            }`}
          >
            ♥ 좋아요 {post.likeCount}
          </button>
        ) : (
          <span className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gray-50 text-gray-400 font-medium text-sm">
            ♥ 좋아요 {post.likeCount}
          </span>
        )}
        {isLoggedIn && (
          <button
            onClick={handleBookmark}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors font-medium text-sm ${
              bookmarked
                ? "bg-orange-50 text-orange-600"
                : "bg-gray-50 text-gray-500 hover:bg-gray-100"
            }`}
          >
            {bookmarked ? "🔖 북마크됨" : "🔖 북마크"}
          </button>
        )}
      </div>

      <CommentSection postId={postId} />
    </div>
  );
}
