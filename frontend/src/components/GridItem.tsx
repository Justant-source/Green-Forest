"use client";

import { useState } from "react";
import Link from "next/link";
import { Post } from "@/types";
import { IMAGE_BASE_URL, toggleBookmark } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import TitleCard from "./TitleCard";

interface GridItemProps {
  post: Post;
  onBookmarkChange?: (postId: number, bookmarked: boolean) => void;
}

export default function GridItem({ post, onBookmarkChange }: GridItemProps) {
  const { isLoggedIn } = useAuth();
  const [bookmarked, setBookmarked] = useState(post.bookmarked ?? false);

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
      {post.status && (
        <span className={`absolute top-2 left-2 z-10 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
          post.status === "COMPLETE" ? "bg-green-500 text-white" :
          post.status === "ING" ? "bg-yellow-400 text-gray-900" :
          "bg-blue-500 text-white"
        }`}>
          {{ REGISTERED: "등록", ING: "진행중", COMPLETE: "완료" }[post.status] || post.status}
        </span>
      )}
      {isLoggedIn && (
        <button
          onClick={handleBookmarkClick}
          className={`absolute top-2 right-2 z-10 w-8 h-8 items-center justify-center rounded-full text-sm hidden [@media(hover:hover)]:flex ${
            bookmarked
              ? "bg-orange-500 text-white [@media(hover:hover)]:flex"
              : "bg-black/50 text-white opacity-0 group-hover:opacity-100"
          }`}
        >
          🔖
        </button>
      )}
      {post.imageUrl ? (
        <div className="relative aspect-[4/5]">
          <img
            src={`${IMAGE_BASE_URL}${post.imageUrl}`}
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
      ) : (
        <div className="relative">
          <TitleCard title={post.title} postId={post.id} category={post.category} likeCount={post.likeCount} />
          {post.status === "COMPLETE" && (
            <div className="absolute inset-0 bg-black/40" />
          )}
        </div>
      )}
    </Link>
  );
}
