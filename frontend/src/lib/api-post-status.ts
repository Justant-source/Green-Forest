import { Post } from "@/types";
import { getToken, logout } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

function authHeaders(): HeadersInit {
  const token = getToken();
  if (token) {
    return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
  }
  return { "Content-Type": "application/json" };
}

function handleUnauthorized(res: Response): void {
  if (res.status === 401 && getToken()) {
    logout();
    window.location.href = "/login";
  }
}

export async function updatePostStatus(id: number, status: string): Promise<Post> {
  const res = await fetch(`${BASE_URL}/posts/${id}/status`, {
    method: "PATCH",
    headers: authHeaders(),
    body: JSON.stringify({ status }),
  });
  if (!res.ok) {
    handleUnauthorized(res);
    throw new Error("Failed to update post status");
  }
  return res.json();
}
