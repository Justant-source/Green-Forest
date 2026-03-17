"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getNotifications, markNotificationAsRead, markAllNotificationsAsRead } from "@/lib/api";
import { Notification, PageResponse } from "@/types";
import { useAuth } from "@/context/AuthContext";

export default function NotificationsPage() {
  const router = useRouter();
  const { isLoggedIn, authLoaded } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    if (!authLoaded) return;
    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }
    fetchNotifications(0, false);
  }, [authLoaded, isLoggedIn, router]);

  const fetchNotifications = async (pageNum: number, append: boolean) => {
    setLoading(true);
    try {
      const data: PageResponse<Notification> = await getNotifications(pageNum, 20);
      setNotifications((prev) => append ? [...prev, ...data.content] : data.content);
      setHasMore(!data.last);
    } catch (error) {
      console.error("Failed to fetch notifications:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (error) {
      console.error("Failed to mark all as read:", error);
    }
  };

  const handleClick = async (notification: Notification) => {
    if (!notification.isRead) {
      await markNotificationAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) => n.id === notification.id ? { ...n, isRead: true } : n)
      );
    }
    if (notification.relatedPostId) {
      router.push(`/posts/${notification.relatedPostId}`);
    } else if (notification.relatedQuestId) {
      router.push("/quests");
    }
  };

  if (!isLoggedIn) return null;

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">알림</h1>
        <button
          onClick={handleMarkAllRead}
          className="text-sm text-forest-500 hover:text-forest-600 transition-colors"
        >
          모두 읽음
        </button>
      </div>

      {loading && notifications.length === 0 ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
        </div>
      ) : notifications.length === 0 ? (
        <div className="text-center py-20 text-gray-400">알림이 없습니다.</div>
      ) : (
        <div className="space-y-2">
          {notifications.map((n) => (
            <button
              key={n.id}
              onClick={() => handleClick(n)}
              className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                n.isRead
                  ? "bg-white border-gray-100"
                  : "bg-forest-50 border-forest-200"
              }`}
            >
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-medium text-forest-500">{n.typeLabel}</span>
                {!n.isRead && <span className="w-2 h-2 bg-accent rounded-full" />}
              </div>
              <div className="text-sm font-medium text-gray-700">{n.title}</div>
              {n.body && <div className="text-xs text-gray-400 mt-0.5">{n.body}</div>}
              <div className="text-xs text-gray-300 mt-1">{new Date(n.createdAt).toLocaleDateString("ko-KR")}</div>
            </button>
          ))}
          {hasMore && (
            <div className="flex justify-center mt-4">
              <button
                onClick={() => {
                  const nextPage = page + 1;
                  setPage(nextPage);
                  fetchNotifications(nextPage, true);
                }}
                disabled={loading}
                className="px-6 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors disabled:opacity-50"
              >
                {loading ? "불러오는 중..." : "더보기"}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
