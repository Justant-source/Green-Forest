import { WeeklyReport } from "@/types/weekly-report";
import { getToken } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

function authHeaders(): HeadersInit {
  const token = getToken();
  if (token) {
    return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
  }
  return { "Content-Type": "application/json" };
}

export async function getMyWeeklyReport(): Promise<WeeklyReport | null> {
  try {
    const res = await fetch(`${BASE_URL}/weekly-reports/me`, {
      headers: authHeaders(),
      cache: "no-store",
    });

    if (res.status === 204) {
      return null; // No content
    }

    if (!res.ok) {
      if (res.status === 401) {
        return null; // Unauthorized, return null instead of throwing
      }
      throw new Error("Failed to fetch weekly report");
    }

    return res.json();
  } catch (error) {
    console.error("Error fetching weekly report:", error);
    return null;
  }
}
