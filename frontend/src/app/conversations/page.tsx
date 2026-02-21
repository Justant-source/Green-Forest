"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ConversationInfo } from "@/types";
import { getConversations } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

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

export default function ConversationsPage() {
  const router = useRouter();
  const { isLoggedIn, authLoaded } = useAuth();
  const [conversations, setConversations] = useState<ConversationInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.push("/login");
      return;
    }
    getConversations()
      .then(setConversations)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [authLoaded, isLoggedIn, router]);

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">대화</h1>
      {conversations.length === 0 ? (
        <p className="text-center text-gray-400 py-12">아직 대화가 없습니다.</p>
      ) : (
        <div className="space-y-2">
          {conversations.map((conv) => (
            <Link
              key={conv.id}
              href={`/conversations/${conv.id}`}
              className="block p-4 bg-white border border-gray-200 rounded-xl hover:border-orange-300 hover:shadow-sm transition-all"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="font-medium text-gray-900 truncate">
                    {conv.otherNickname}
                  </span>
                  {conv.otherLeft && (
                    <span className="text-xs text-gray-400 shrink-0">(나감)</span>
                  )}
                </div>
                <span className="text-xs text-gray-400 shrink-0 ml-2">
                  {getRelativeTime(conv.updatedAt)}
                </span>
              </div>
              <p className="text-sm text-gray-500 mt-1 truncate">
                {conv.lastMessage || "대화를 시작하세요"}
              </p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
