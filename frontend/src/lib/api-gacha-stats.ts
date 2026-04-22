import { GachaStats } from "@/types";
import { getToken } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

function authHeaders(): HeadersInit {
  const token = getToken();
  if (token) {
    return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
  }
  return { "Content-Type": "application/json" };
}

export async function getGachaStats(): Promise<GachaStats> {
  const res = await fetch(`${BASE_URL}/gacha/stats`, {
    headers: authHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw res;
  return res.json();
}
