import { getToken } from "@/lib/auth";
import {
  CreateEventRequest,
  EventModeResponse,
  PhotoBingoActivity,
  PhotoBingoSubmissionDto,
  Event,
} from "./types";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

function authHeaders(json = true): HeadersInit {
  const token = getToken();
  const h: Record<string, string> = {};
  if (json) h["Content-Type"] = "application/json";
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let msg = "요청에 실패했습니다.";
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch {
      /* ignore */
    }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as unknown as T;
  return res.json() as Promise<T>;
}

// ---------- 사용자 API ----------

export async function getEventMode(): Promise<EventModeResponse> {
  const res = await fetch(`${BASE_URL}/events/mode`, {
    cache: "no-store",
    headers: authHeaders(),
  });
  return handle(res);
}

export async function listEvents(status?: string): Promise<Event[]> {
  const qs = status ? `?status=${encodeURIComponent(status)}` : "";
  const res = await fetch(`${BASE_URL}/events${qs}`, {
    cache: "no-store",
    headers: authHeaders(),
  });
  return handle(res);
}

export async function getEvent(id: number): Promise<Event> {
  const res = await fetch(`${BASE_URL}/events/${id}`, {
    cache: "no-store",
    headers: authHeaders(),
  });
  return handle(res);
}

export async function getMySubmission(eventId: number): Promise<PhotoBingoSubmissionDto> {
  const res = await fetch(
    `${BASE_URL}/events/${eventId}/photo-bingo/my-submission`,
    { cache: "no-store", headers: authHeaders() }
  );
  return handle(res);
}

export async function updateCaption(eventId: number, caption: string): Promise<PhotoBingoSubmissionDto> {
  const res = await fetch(
    `${BASE_URL}/events/${eventId}/photo-bingo/my-submission`,
    { method: "PATCH", headers: authHeaders(), body: JSON.stringify({ caption }) }
  );
  return handle(res);
}

export async function uploadCellImage(
  eventId: number,
  cellIndex: number,
  file: File
): Promise<PhotoBingoSubmissionDto> {
  const token = getToken();
  const fd = new FormData();
  fd.append("image", file);
  const res = await fetch(
    `${BASE_URL}/events/${eventId}/photo-bingo/cells/${cellIndex}/image`,
    {
      method: "PUT",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    }
  );
  return handle(res);
}

export async function deleteCellImage(eventId: number, cellIndex: number): Promise<PhotoBingoSubmissionDto> {
  const res = await fetch(
    `${BASE_URL}/events/${eventId}/photo-bingo/cells/${cellIndex}/image`,
    { method: "DELETE", headers: authHeaders() }
  );
  return handle(res);
}

export async function getBingoActivity(eventId: number, limit = 20): Promise<PhotoBingoActivity[]> {
  const res = await fetch(
    `${BASE_URL}/events/${eventId}/photo-bingo/activity?limit=${limit}`,
    { cache: "no-store", headers: authHeaders() }
  );
  return handle(res);
}

// ---------- 관리자 API ----------

export async function adminCreateEvent(req: CreateEventRequest): Promise<Event> {
  const res = await fetch(`${BASE_URL}/admin/events`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(req),
  });
  return handle(res);
}

export async function adminChangeStatus(id: number, status: string): Promise<Event> {
  const res = await fetch(`${BASE_URL}/admin/events/${id}/status`, {
    method: "PATCH",
    headers: authHeaders(),
    body: JSON.stringify({ status }),
  });
  return handle(res);
}

export async function adminExtendEvent(id: number, additionalMinutes: number): Promise<Event> {
  const res = await fetch(`${BASE_URL}/admin/events/${id}/extend`, {
    method: "PATCH",
    headers: authHeaders(),
    body: JSON.stringify({ additionalMinutes }),
  });
  return handle(res);
}

export async function adminListSubmissions(eventId: number): Promise<PhotoBingoSubmissionDto[]> {
  const res = await fetch(`${BASE_URL}/admin/events/${eventId}/photo-bingo/submissions`, {
    cache: "no-store",
    headers: authHeaders(),
  });
  return handle(res);
}

export async function adminGetSubmission(eventId: number, submissionId: number): Promise<PhotoBingoSubmissionDto> {
  const res = await fetch(
    `${BASE_URL}/admin/events/${eventId}/photo-bingo/submissions/${submissionId}`,
    { cache: "no-store", headers: authHeaders() }
  );
  return handle(res);
}

export async function adminScoreCell(
  eventId: number,
  cellId: number,
  scoreStatus: "APPROVED" | "REJECTED",
  comment?: string
): Promise<PhotoBingoSubmissionDto> {
  const res = await fetch(
    `${BASE_URL}/admin/events/${eventId}/photo-bingo/cells/${cellId}/score`,
    {
      method: "PUT",
      headers: authHeaders(),
      body: JSON.stringify({ scoreStatus, comment }),
    }
  );
  return handle(res);
}

export async function adminFinalizeEvent(eventId: number): Promise<Event> {
  const res = await fetch(`${BASE_URL}/admin/events/${eventId}/finalize`, {
    method: "POST",
    headers: authHeaders(),
  });
  return handle(res);
}
