"use client";

import { useState } from "react";
import Link from "next/link";
import { Post } from "@/types";
import { toMediaUrl, toggleBookmark, mediaSrcSet, MediaSize } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useCategories } from "@/context/CategoryContext";
import TitleCard from "./TitleCard";
import { parsePhotoBingoMarker } from "@/lib/events/postMarker";

interface GridItemProps {
  post: Post;
  onBookmarkChange?: (postId: number, bookmarked: boolean) => void;
}

export default function GridItem({ post, onBookmarkChange }: GridItemProps) {
  const { isLoggedIn, authLoaded } = useAuth();
  const { categories } = useCategories();
  const [bookmarked, setBookmarked] = useState(post.bookmarked ?? false);

  const getCategoryBadgeStyle = (): { bg: string; label: string } => {
    const cat = categories.find((c) => c.name === post.category);
    if (!cat) return { bg: "bg-gray-400", label: post.category[0] || "?" };

    const labelMap: Record<string, string> = {
      "긍정문구": "긍",
      "동료칭찬": "칭",
      "퀘스트": "퀘",
      "이벤트": "이",
      "survey": "설",
    };

    const colorMap: Record<string, string> = {
      green: "bg-green-500",
      blue: "bg-blue-500",
      yellow: "bg-yellow-500",
      red: "bg-red-500",
      purple: "bg-purple-500",
      pink: "bg-pink-500",
      indigo: "bg-indigo-500",
      orange: "bg-orange-500",
    };

    return {
      bg: colorMap[cat.color] || "bg-gray-400",
      label: labelMap[cat.name] || cat.name[0],
    };
  };

  const badgeStyle = getCategoryBadgeStyle();

  const handleBookmarkClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      const res = await toggleBookmark(post.id);
      setBookmarked(res.bookmarked);
      onBookmarkChange?.(post.id, res.bookmarked);
    } catch (error) {
      console.error("Failed to toggle bookmark:", error);
    }
  };

  return (
    <Link
      href={`/posts/${post.id}`}
      className="group block overflow-hidden transition-transform duration-200 hover:scale-105 relative"
    >
      <span className={`absolute top-2 left-2 z-10 w-6 h-6 rounded-full ${badgeStyle.bg} text-white text-[10px] font-semibold flex items-center justify-center`}>
        {badgeStyle.label}
      </span>
      {post.status && (
        <span className={`absolute top-2 left-10 z-10 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
          post.status === "COMPLETE" ? "bg-green-500 text-white" :
          post.status === "ING" ? "bg-yellow-400 text-gray-900" :
          "bg-blue-500 text-white"
        }`}>
          {{ REGISTERED: "등록", ING: "진행중", COMPLETE: "완료" }[post.status] || post.status}
        </span>
      )}
      {authLoaded && isLoggedIn && (
        <button
          onClick={handleBookmarkClick}
          className={`absolute top-2 right-2 z-10 w-8 h-8 items-center justify-center rounded-full text-sm hidden [@media(hover:hover)]:flex ${
            bookmarked
              ? "bg-forest-500 text-white [@media(hover:hover)]:flex"
              : "bg-black/50 text-white opacity-0 group-hover:opacity-100"
          }`}
        >
          🔖
        </button>
      )}
      {(() => {
        // 설문 이미지 썸네일 (이미지 옵션이 있는 설문)
        if (post.category === "survey" && post.imageUrls && post.imageUrls.length > 0) {
          return <SurveyImageThumbnail imageUrls={post.imageUrls} likeCount={post.likeCount} />;
        }

        const bingo = parsePhotoBingoMarker(post.content).bingo;
        if (bingo) {
          const cells = [...bingo.cells].sort((a, b) => a.idx - b.idx);
          const uploaded = cells.filter((c) => c.imageUrl).length;
          return (
            <div className="relative aspect-[4/5] bg-forest-50">
              <div className="absolute inset-0 grid grid-cols-3 grid-rows-3 gap-[2px] p-[2px]">
                {cells.map((c) => (
                  <div key={c.idx} className="relative bg-white overflow-hidden">
                    {c.imageUrl ? (
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img
                        src={toMediaUrl(c.imageUrl, "sm")}
                        alt={c.theme}
                        loading="lazy"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full bg-gray-100 flex items-center justify-center">
                        <span className="text-gray-300 text-[10px]">·</span>
                      </div>
                    )}
                    {bingo.eventStatus === "SCORED" && c.scoreStatus === "APPROVED" && c.imageUrl && (
                      <div className="absolute inset-0 ring-2 ring-green-500 ring-inset pointer-events-none" />
                    )}
                  </div>
                ))}
              </div>
              <div className="absolute top-2 right-2 z-10 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-forest-600/90 text-white">
                {uploaded}/9
              </div>
              {post.status === "COMPLETE" && (
                <div className="absolute inset-0 bg-black/40" />
              )}
              <div className="absolute bottom-2 right-2 text-[10px] sm:text-xs text-white bg-black/50 rounded-full px-2 py-0.5">
                <span>♥ {post.likeCount}</span>
              </div>
            </div>
          );
        }
        if (post.imageUrl) {
          const srcSet = mediaSrcSet(post.imageUrl);
          return (
            <div className="relative aspect-[4/5]">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={srcSet ? srcSet.src : toMediaUrl(post.imageUrl)}
                srcSet={srcSet?.srcSet}
                sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 240px"
                loading="lazy"
                alt={post.title}
                className="w-full h-full object-cover"
              />
              {post.status === "COMPLETE" && (
                <div className="absolute inset-0 bg-black/40" />
              )}
              <div className="absolute bottom-2 right-2 text-[10px] sm:text-xs text-white bg-black/50 rounded-full px-2 py-0.5">
                <span>♥ {post.likeCount}</span>
              </div>
            </div>
          );
        }
        return (
          <div className="relative">
            <TitleCard title={post.title} postId={post.id} category={post.category} likeCount={post.likeCount} />
            {post.status === "COMPLETE" && (
              <div className="absolute inset-0 bg-black/40" />
            )}
          </div>
        );
      })()}
    </Link>
  );
}

/**
 * 설문 이미지 옵션 썸네일 그리드
 *
 * 레이아웃 규칙:
 * N=1: 단일 이미지
 * N=2: 2열 나란히
 * N=3: 3열 나란히
 * N=4: 2×2 격자
 * N=5: 3×2, 6번째 셀 = "5장" 라벨
 * N=6: 3×2 모두 채움
 * N=7: 3×2, 6번째 셀 = "+2" 오버레이 (5장 보임)
 * N=8: 3×2, 6번째 셀 = "+3" 오버레이 (5장 보임)
 * N=9: 3×3 모두 채움
 */
function SurveyImageThumbnail({
  imageUrls,
  likeCount,
}: {
  imageUrls: string[];
  likeCount: number;
}) {
  const n = Math.min(imageUrls.length, 9);

  const getGrid = (n: number): { cols: number; rows: number } => {
    if (n <= 2) return { cols: n, rows: 1 };
    if (n === 3) return { cols: 3, rows: 1 };
    if (n === 4) return { cols: 2, rows: 2 };
    if (n <= 6) return { cols: 3, rows: 2 };
    return { cols: 3, rows: 3 };
  };

  const { cols, rows } = getGrid(n);
  const totalCells = cols * rows;

  // 마지막 셀에 라벨/오버레이가 필요한 경우 (n < totalCells 이거나 총개수 초과)
  const needsLastLabel = n < totalCells || imageUrls.length > totalCells;
  const imagesShown = needsLastLabel ? totalCells - 1 : n;
  const hiddenCount = imageUrls.length - imagesShown;

  const getLastCellLabel = () => {
    if (n === 5) return "5장";
    if (hiddenCount > 0) return `+${hiddenCount}`;
    return null;
  };

  const lastLabel = getLastCellLabel();

  return (
    <div className="relative aspect-[4/5] bg-gray-200">
      <div
        className="absolute inset-0 p-[2px]"
        style={{
          display: "grid",
          gridTemplateColumns: `repeat(${cols}, 1fr)`,
          gridTemplateRows: `repeat(${rows}, 1fr)`,
          gap: "2px",
        }}
      >
        {imageUrls.slice(0, imagesShown).map((url, i) => (
          <div key={i} className="relative overflow-hidden bg-gray-300">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={toMediaUrl(url, "sm" as MediaSize)}
              loading="lazy"
              alt=""
              className="w-full h-full object-cover"
            />
          </div>
        ))}
        {lastLabel && (
          <div className="flex items-center justify-center bg-gray-800/75 text-white">
            <span className="font-semibold text-sm">{lastLabel}</span>
          </div>
        )}
      </div>
      {/* 좋아요 카운트 */}
      <div className="absolute bottom-2 right-2 text-[10px] sm:text-xs text-white bg-black/50 rounded-full px-2 py-0.5">
        <span>♥ {likeCount}</span>
      </div>
    </div>
  );
}
