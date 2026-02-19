"use client";

import { useState, useEffect } from "react";
import { Comment } from "@/types";
import { getComments, addComment } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import Link from "next/link";

interface CommentSectionProps {
  postId: number;
}

function getRelativeTime(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr.endsWith("Z") ? dateStr : dateStr + "Z");
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffMin < 1) return "방금 전";
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  if (diffDay < 30) return `${diffDay}일 전`;
  return date.toLocaleDateString("ko-KR");
}

export default function CommentSection({ postId }: CommentSectionProps) {
  const { isLoggedIn, nickname } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getComments(postId).then(setComments).catch(console.error);
  }, [postId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    setSubmitting(true);
    try {
      const newComment = await addComment(postId, content.trim());
      setComments((prev) => [newComment, ...prev]);
      setContent("");
    } catch (error) {
      console.error("Failed to add comment:", error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 pt-6 border-t">
      <h2 className="text-lg font-bold">댓글 {comments.length}개</h2>

      {isLoggedIn ? (
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="text-sm text-gray-600 font-medium">{nickname}</div>
          <textarea
            placeholder="댓글을 입력하세요..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={3}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-500"
          />
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={submitting || !content.trim()}
              className="px-4 py-2 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {submitting ? "등록 중..." : "댓글 등록"}
            </button>
          </div>
        </form>
      ) : (
        <div className="text-center py-4 bg-gray-50 rounded-lg">
          <p className="text-sm text-gray-500">
            <Link href="/login" className="text-orange-600 hover:underline">
              로그인
            </Link>
            {" "}후 댓글을 작성할 수 있습니다.
          </p>
        </div>
      )}

      <div className="space-y-4">
        {comments.map((comment) => (
          <div key={comment.id} className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="font-medium text-sm text-gray-900">
                {comment.authorName}
              </span>
              <span className="text-xs text-gray-400">
                {getRelativeTime(comment.createdAt)}
              </span>
            </div>
            <p className="text-sm text-gray-700">{comment.content}</p>
          </div>
        ))}

        {comments.length === 0 && (
          <p className="text-center text-gray-400 text-sm py-8">
            아직 댓글이 없습니다. 첫 번째 댓글을 남겨보세요!
          </p>
        )}
      </div>
    </div>
  );
}
