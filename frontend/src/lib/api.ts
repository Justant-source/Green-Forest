import { Post, Comment, PageResponse, CategoryInfo, CategoryRequestInfo, ConversationInfo, ChatMessage } from "@/types";
import { getToken } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
export const IMAGE_BASE_URL = process.env.NEXT_PUBLIC_IMAGE_BASE_URL || "http://localhost:8080";

function authHeaders(): HeadersInit {
  const token = getToken();
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

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
  if (!res.ok) throw new Error("Failed to create post");
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

export async function getPendingCategoryRequests(): Promise<
  CategoryRequestInfo[]
> {
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

// 대화(쪽지) API
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
