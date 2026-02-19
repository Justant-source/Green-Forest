"use client";

import { useState, useEffect } from "react";
import { Comment } from "@/types";
import { getComments, addComment, getCommentCount, updateComment, deleteComment } from "@/lib/api";
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

const DEPTH_MARGIN: Record<number, string> = {
  0: "ml-0",
  1: "ml-4",
  2: "ml-8",
  3: "ml-10",
};

function getDepthMargin(depth: number): string {
  if (depth >= 3) return DEPTH_MARGIN[3];
  return DEPTH_MARGIN[depth] || DEPTH_MARGIN[3];
}

interface CommentItemProps {
  comment: Comment;
  depth: number;
  postId: number;
  isLoggedIn: boolean;
  nickname: string | null;
  ancestorDeleted: boolean;
  onReplyAdded: (parentId: number, newReply: Comment) => void;
  onCommentUpdated: (commentId: number, updated: Comment) => void;
  onCommentDeleted: (commentId: number, updated: Comment) => void;
}

function CommentItem({
  comment,
  depth,
  postId,
  isLoggedIn,
  nickname,
  ancestorDeleted,
  onReplyAdded,
  onCommentUpdated,
  onCommentDeleted,
}: CommentItemProps) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyContent, setReplyContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [editing, setEditing] = useState(false);

  const isOwner = isLoggedIn && nickname === comment.authorName;
  const replyBlocked = comment.deleted || ancestorDeleted;

  const handleReplySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyContent.trim()) return;

    setSubmitting(true);
    try {
      const newReply = await addComment(postId, replyContent.trim(), comment.id);
      onReplyAdded(comment.id, newReply);
      setReplyContent("");
      setShowReplyForm(false);
    } catch (error) {
      console.error("Failed to add reply:", error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editContent.trim()) return;

    setEditing(true);
    try {
      const updated = await updateComment(postId, comment.id, editContent.trim());
      onCommentUpdated(comment.id, updated);
      setShowEditForm(false);
    } catch (error) {
      console.error("Failed to update comment:", error);
    } finally {
      setEditing(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      const updated = await deleteComment(postId, comment.id);
      onCommentDeleted(comment.id, updated);
    } catch (error) {
      console.error("Failed to delete comment:", error);
    }
  };

  return (
    <div className={getDepthMargin(depth)}>
      <div className={`rounded-lg p-4 ${comment.deleted ? "bg-gray-100" : "bg-gray-50"}`}>
        <div className="flex items-center justify-between mb-2">
          <span className={`font-medium text-sm ${comment.deleted ? "text-gray-400" : "text-gray-900"}`}>
            {comment.deleted ? "" : comment.authorName}
          </span>
          <span className="text-xs text-gray-400">
            {getRelativeTime(comment.createdAt)}
            {!comment.deleted && comment.updatedAt && " (수정됨)"}
          </span>
        </div>
        <p className={`text-sm ${comment.deleted ? "text-gray-400 italic" : "text-gray-700"}`}>
          {comment.content}
        </p>
        {!comment.deleted && (
          <div className="flex gap-3 mt-2">
            {isLoggedIn && !replyBlocked && (
              <button
                onClick={() => { setShowReplyForm(!showReplyForm); setShowEditForm(false); }}
                className="text-xs text-gray-500 hover:text-orange-600 transition-colors"
              >
                {showReplyForm ? "취소" : "답글"}
              </button>
            )}
            {isOwner && (
              <>
                <button
                  onClick={() => { setShowEditForm(!showEditForm); setShowReplyForm(false); setEditContent(comment.content); }}
                  className="text-xs text-gray-500 hover:text-blue-600 transition-colors"
                >
                  {showEditForm ? "취소" : "수정"}
                </button>
                <button
                  onClick={handleDelete}
                  className="text-xs text-gray-500 hover:text-red-600 transition-colors"
                >
                  삭제
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {showEditForm && (
        <form onSubmit={handleEditSubmit} className="mt-2 ml-4 space-y-2">
          <textarea
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            rows={2}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowEditForm(false)}
              className="px-3 py-1.5 text-gray-600 rounded-lg text-xs font-medium hover:bg-gray-100 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={editing || !editContent.trim()}
              className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {editing ? "수정 중..." : "수정 완료"}
            </button>
          </div>
        </form>
      )}

      {showReplyForm && (
        <form onSubmit={handleReplySubmit} className="mt-2 ml-4 space-y-2">
          <div className="text-xs text-gray-600 font-medium">{nickname}</div>
          <textarea
            placeholder="답글을 입력하세요..."
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            rows={2}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-500"
          />
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={submitting || !replyContent.trim()}
              className="px-3 py-1.5 bg-orange-600 text-white rounded-lg text-xs font-medium hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {submitting ? "등록 중..." : "답글 등록"}
            </button>
          </div>
        </form>
      )}

      {comment.replies && comment.replies.length > 0 && (
        <div className="mt-2 space-y-2">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              postId={postId}
              isLoggedIn={isLoggedIn}
              nickname={nickname}
              ancestorDeleted={replyBlocked}
              onReplyAdded={onReplyAdded}
              onCommentUpdated={onCommentUpdated}
              onCommentDeleted={onCommentDeleted}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function addReplyToTree(comments: Comment[], parentId: number, newReply: Comment): Comment[] {
  return comments.map((comment) => {
    if (comment.id === parentId) {
      return {
        ...comment,
        replies: [...(comment.replies || []), newReply],
      };
    }
    if (comment.replies && comment.replies.length > 0) {
      return {
        ...comment,
        replies: addReplyToTree(comment.replies, parentId, newReply),
      };
    }
    return comment;
  });
}

function updateCommentInTree(comments: Comment[], commentId: number, updated: Comment): Comment[] {
  return comments.map((comment) => {
    if (comment.id === commentId) {
      return { ...updated, replies: comment.replies };
    }
    if (comment.replies && comment.replies.length > 0) {
      return {
        ...comment,
        replies: updateCommentInTree(comment.replies, commentId, updated),
      };
    }
    return comment;
  });
}

export default function CommentSection({ postId }: CommentSectionProps) {
  const { isLoggedIn, nickname } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getComments(postId).then(setComments).catch(console.error);
    getCommentCount(postId).then(setTotalCount).catch(console.error);
  }, [postId]);

  const handleReplyAdded = (parentId: number, newReply: Comment) => {
    setComments((prev) => addReplyToTree(prev, parentId, { ...newReply, replies: newReply.replies || [] }));
    setTotalCount((prev) => prev + 1);
  };

  const handleCommentUpdated = (commentId: number, updated: Comment) => {
    setComments((prev) => updateCommentInTree(prev, commentId, updated));
  };

  const handleCommentDeleted = (commentId: number, updated: Comment) => {
    setComments((prev) => updateCommentInTree(prev, commentId, updated));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    setSubmitting(true);
    try {
      const newComment = await addComment(postId, content.trim());
      setComments((prev) => [{ ...newComment, replies: newComment.replies || [] }, ...prev]);
      setTotalCount((prev) => prev + 1);
      setContent("");
    } catch (error) {
      console.error("Failed to add comment:", error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 pt-6 border-t">
      <h2 className="text-lg font-bold">댓글 {totalCount}개</h2>

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
          <CommentItem
            key={comment.id}
            comment={comment}
            depth={0}
            postId={postId}
            isLoggedIn={isLoggedIn}
            nickname={nickname}
            ancestorDeleted={false}
            onReplyAdded={handleReplyAdded}
            onCommentUpdated={handleCommentUpdated}
            onCommentDeleted={handleCommentDeleted}
          />
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
