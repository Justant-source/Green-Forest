import {
  Post, Comment, PageResponse, CategoryInfo, CategoryRequestInfo,
  ConversationInfo, ChatMessage, User, Quest, Notification,
  LeaderboardEntry, PartyMember, DropTransaction,
  AdminUser, AdminParty, AdminStats,
} from "@/types";
import { getToken, logout } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
export const IMAGE_BASE_URL = process.env.NEXT_PUBLIC_IMAGE_BASE_URL || "http://localhost:8080";

function authHeaders(): HeadersInit {
  const token = getToken();
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

function handleUnauthorized(res: Response): void {
  if (res.status === 401 && getToken()) {
    logout();
    window.location.href = "/login";
  }
}

// ========== 포스트 ==========

export async function getPosts(
  category?: string,
  sort?: string,
  page?: number,
  size?: number,
  status?: string
): Promise<PageResponse<Post>> {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  if (sort) params.set("sort", sort);
  if (status) params.set("status", status);
  if (page !== undefined) params.set("page", String(page));
  if (size !== undefined) params.set("size", String(size));

  const res = await fetch(`${BASE_URL}/posts?${params.toString()}`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch posts");
  return res.json();
}

export async function getPost(id: number): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts/${id}`, { cache: "no-store" });
  if (!res.ok) throw new Error("Failed to fetch post");
  return res.json();
}

export async function createPost(formData: FormData): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts`, {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });
  if (!res.ok) {
    handleUnauthorized(res);
    throw new Error("Failed to create post");
  }
  return res.json();
}

export async function updatePostStatus(id: number, status: string): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts/${id}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ status }),
  });
  if (!res.ok) throw new Error("Failed to update post status");
  return res.json();
}

export async function updatePost(id: number, formData: FormData): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts/${id}`, {
    method: "PUT",
    headers: authHeaders(),
    body: formData,
  });
  if (!res.ok) throw new Error("Failed to update post");
  return res.json();
}

export async function deletePost(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/posts/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to delete post");
}

export async function toggleLike(id: number): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts/${id}/like`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to toggle like");
  return res.json();
}

export async function getLikeStatus(
  postId: number
): Promise<{ liked: boolean }> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/like`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to get like status");
  return res.json();
}

export async function getComments(postId: number): Promise<Comment[]> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/comments`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch comments");
  return res.json();
}

export async function addComment(
  postId: number,
  content: string,
  parentId?: number
): Promise<Comment> {
  const body: { content: string; parentId?: number } = { content };
  if (parentId !== undefined) body.parentId = parentId;
  const res = await fetch(`${BASE_URL}/posts/${postId}/comments`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error("Failed to add comment");
  return res.json();
}

export async function updateComment(
  postId: number,
  commentId: number,
  content: string
): Promise<Comment> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/comments/${commentId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error("Failed to update comment");
  return res.json();
}

export async function deleteComment(
  postId: number,
  commentId: number
): Promise<Comment> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/comments/${commentId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to delete comment");
  return res.json();
}

export async function getCommentCount(
  postId: number
): Promise<number> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/comments/count`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch comment count");
  const data = await res.json();
  return data.count;
}

export async function toggleBookmark(
  postId: number
): Promise<{ bookmarked: boolean }> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/bookmark`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to toggle bookmark");
  return res.json();
}

export async function getBookmarkStatus(
  postId: number
): Promise<{ bookmarked: boolean }> {
  const res = await fetch(`${BASE_URL}/posts/${postId}/bookmark`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to get bookmark status");
  return res.json();
}

// ========== 프로필 ==========

export async function getMyPosts(
  page: number = 0,
  size: number = 12
): Promise<PageResponse<Post>> {
  const res = await fetch(
    `${BASE_URL}/profile/posts?page=${page}&size=${size}`,
    { headers: authHeaders(), cache: "no-store" }
  );
  if (!res.ok) throw new Error("Failed to fetch my posts");
  return res.json();
}

export async function getMyBookmarks(
  page: number = 0,
  size: number = 12
): Promise<PageResponse<Post>> {
  const res = await fetch(
    `${BASE_URL}/profile/bookmarks?page=${page}&size=${size}`,
    { headers: authHeaders(), cache: "no-store" }
  );
  if (!res.ok) throw new Error("Failed to fetch bookmarks");
  return res.json();
}

// ========== 카테고리 ==========

export async function getCategories(): Promise<CategoryInfo[]> {
  const res = await fetch(`${BASE_URL}/categories`, { cache: "no-store" });
  if (!res.ok) throw new Error("Failed to fetch categories");
  return res.json();
}

export async function requestCategory(data: {
  name: string;
}): Promise<CategoryRequestInfo> {
  const res = await fetch(`${BASE_URL}/categories/request`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to request category");
  return res.json();
}

// ========== 유저 ==========

export async function getMe(): Promise<User> {
  const res = await fetch(`${BASE_URL}/users/me`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) {
    handleUnauthorized(res);
    throw new Error("Failed to fetch user");
  }
  return res.json();
}

export async function searchUsers(q: string): Promise<{ id: number; name: string; nickname: string }[]> {
  const res = await fetch(`${BASE_URL}/users/search?q=${encodeURIComponent(q)}`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) return [];
  return res.json();
}

export async function getUserById(id: number): Promise<User> {
  const res = await fetch(`${BASE_URL}/users/${id}`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch user");
  return res.json();
}

export async function updateMyProfile(data: {
  nickname?: string;
  plantName?: string;
  plantType?: string;
}): Promise<User> {
  const res = await fetch(`${BASE_URL}/users/me/profile`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update profile");
  return res.json();
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/users/me/password`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => null);
    throw new Error(data?.message || "비밀번호 변경에 실패했습니다.");
  }
}

export async function getMyDropHistory(
  page: number = 0,
  size: number = 20
): Promise<PageResponse<DropTransaction>> {
  const res = await fetch(
    `${BASE_URL}/users/me/drops?page=${page}&size=${size}`,
    { headers: authHeaders(), cache: "no-store" }
  );
  if (!res.ok) throw new Error("Failed to fetch drop history");
  return res.json();
}

export async function giftDrops(receiverNickname: string, amount: number): Promise<{ status: string; newTotal: string }> {
  const res = await fetch(`${BASE_URL}/users/me/gift`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ receiverNickname, amount }),
  });
  if (!res.ok) throw new Error("Failed to gift drops");
  return res.json();
}

// ========== 리더보드 ==========

export async function getLeaderboard(period?: string): Promise<LeaderboardEntry[]> {
  const params = new URLSearchParams();
  if (period) params.set("period", period);
  const res = await fetch(`${BASE_URL}/leaderboard?${params.toString()}`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch leaderboard");
  return res.json();
}

export async function getPartyMembers(partyId: number, period?: string): Promise<PartyMember[]> {
  const params = new URLSearchParams();
  if (period) params.set("period", period);
  const res = await fetch(`${BASE_URL}/leaderboard/party/${partyId}?${params.toString()}`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch party members");
  return res.json();
}

// ========== 퀘스트 ==========

export async function getQuests(): Promise<Quest[]> {
  const res = await fetch(`${BASE_URL}/quests`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch quests");
  return res.json();
}

export async function getQuest(id: number): Promise<Quest> {
  const res = await fetch(`${BASE_URL}/quests/${id}`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch quest");
  return res.json();
}

export async function castVote(questId: number, votedForOption: string): Promise<{ status: string }> {
  const res = await fetch(`${BASE_URL}/quests/${questId}/vote`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ votedForOption }),
  });
  if (!res.ok) throw new Error("Failed to cast vote");
  return res.json();
}

export async function getVoteResults(questId: number): Promise<Record<string, number>> {
  const res = await fetch(`${BASE_URL}/quests/${questId}/votes`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch vote results");
  return res.json();
}

// ========== 알림 ==========

export async function getNotifications(
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Notification>> {
  const res = await fetch(
    `${BASE_URL}/notifications?page=${page}&size=${size}`,
    { headers: authHeaders(), cache: "no-store" }
  );
  if (!res.ok) throw new Error("Failed to fetch notifications");
  return res.json();
}

export async function getUnreadNotificationCount(): Promise<number> {
  const res = await fetch(`${BASE_URL}/notifications/unread-count`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch unread count");
  const data = await res.json();
  return data.count;
}

export async function markNotificationAsRead(id: number): Promise<void> {
  await fetch(`${BASE_URL}/notifications/${id}/read`, {
    method: "PUT",
    headers: authHeaders(),
  });
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await fetch(`${BASE_URL}/notifications/read-all`, {
    method: "PUT",
    headers: authHeaders(),
  });
}

// ========== 대화(쪽지) ==========

export async function startConversation(nickname: string): Promise<{ conversationId: number }> {
  const res = await fetch(`${BASE_URL}/conversations`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ nickname }),
  });
  if (!res.ok) throw new Error("Failed to start conversation");
  return res.json();
}

export async function getConversations(): Promise<ConversationInfo[]> {
  const res = await fetch(`${BASE_URL}/conversations`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch conversations");
  return res.json();
}

export async function getConversationMessages(conversationId: number): Promise<ChatMessage[]> {
  const res = await fetch(`${BASE_URL}/conversations/${conversationId}/messages`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch messages");
  return res.json();
}

export async function sendChatMessage(conversationId: number, content: string): Promise<ChatMessage> {
  const res = await fetch(`${BASE_URL}/conversations/${conversationId}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error("Failed to send message");
  return res.json();
}

export async function leaveConversation(conversationId: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/conversations/${conversationId}/leave`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to leave conversation");
}

// ========== 관리자 ==========

export async function getAdminCategories(): Promise<CategoryInfo[]> {
  const res = await fetch(`${BASE_URL}/admin/categories`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch categories");
  return res.json();
}

export async function createAdminCategory(data: {
  name: string;
  label: string;
  color: string;
  hasStatus?: boolean;
}): Promise<CategoryInfo> {
  const res = await fetch(`${BASE_URL}/admin/categories`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to create category");
  return res.json();
}

export async function deleteAdminCategory(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/categories/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to delete category");
}

export async function getPendingCategoryRequests(): Promise<CategoryRequestInfo[]> {
  const res = await fetch(`${BASE_URL}/admin/category-requests`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch category requests");
  return res.json();
}

export async function approveCategoryRequest(
  id: number,
  data: { label: string; color: string; hasStatus: boolean }
): Promise<CategoryInfo> {
  const res = await fetch(`${BASE_URL}/admin/category-requests/${id}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to approve category request");
  return res.json();
}

export async function rejectCategoryRequest(
  id: number,
  reason: string
): Promise<CategoryRequestInfo> {
  const res = await fetch(`${BASE_URL}/admin/category-requests/${id}/reject`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ reason }),
  });
  if (!res.ok) throw new Error("Failed to reject category request");
  return res.json();
}

export async function getAdminUsers(): Promise<AdminUser[]> {
  const res = await fetch(`${BASE_URL}/admin/users`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch users");
  return res.json();
}

export async function updateAdminUser(id: number, data: {
  partyId?: number | null;
  plantType?: string;
  nickname?: string;
}): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/users/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update user");
}

export async function resetAdminUserPassword(id: number): Promise<{ status: string; tempPassword: string }> {
  const res = await fetch(`${BASE_URL}/admin/users/${id}/reset-password`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to reset password");
  return res.json();
}

export async function getAdminParties(): Promise<AdminParty[]> {
  const res = await fetch(`${BASE_URL}/admin/parties`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch parties");
  return res.json();
}

export async function createAdminParty(name: string): Promise<{ id: number; name: string }> {
  const res = await fetch(`${BASE_URL}/admin/parties`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw new Error("Failed to create party");
  return res.json();
}

export async function updateAdminParty(id: number, name: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/parties/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw new Error("Failed to update party");
}

export async function deleteAdminParty(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/parties/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to delete party");
}

export async function awardDrops(userId: number, amount: number, reason: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/drops/award`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ userId, amount, reason }),
  });
  if (!res.ok) throw new Error("Failed to award drops");
}

export async function deductDrops(userId: number, amount: number, reason: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/drops/deduct`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ userId, amount, reason }),
  });
  if (!res.ok) throw new Error("Failed to deduct drops");
}

export async function createAdminQuest(data: {
  title: string;
  description: string;
  rewardDrops: number;
  startDate: string;
  endDate: string;
  targetType?: string;
  targetPartyId?: number | null;
  maxCompletionsPerUser?: number;
  isVoteType?: boolean;
}): Promise<{ id: number; title: string; status: string }> {
  const res = await fetch(`${BASE_URL}/admin/quests`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to create quest");
  return res.json();
}

export async function updateAdminQuest(id: number, data: {
  title: string;
  description: string;
  rewardDrops: number;
  startDate: string;
  endDate: string;
  targetType?: string;
  targetPartyId?: number | null;
  maxCompletionsPerUser?: number;
  isActive?: boolean;
}): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/quests/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update quest");
}

export async function deleteAdminQuest(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/quests/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("Failed to delete quest");
}

export async function getAdminStats(): Promise<AdminStats> {
  const res = await fetch(`${BASE_URL}/admin/stats`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Failed to fetch stats");
  return res.json();
}

export async function createAnnouncement(title: string, content: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/admin/announcements`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ title, content }),
  });
  if (!res.ok) throw new Error("Failed to create announcement");
}
