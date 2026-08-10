import { getToken } from "@/lib/auth";
import {
  CreateEventRequest,
  EventModeResponse,
  PhotoBingoActivity,
  PhotoBingoSubmissionDto,
  Event,
  PhotoExhibitionSubmission,
  PhotoExhibitionAdminSubmission, PhotoExhibitionPreview, PhotoExhibitionVoterAudit,
} from "./types";

export const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

export function authHeaders(json = true): HeadersInit {
  const token = getToken();
  const h: Record<string, string> = {};
  if (json) h["Content-Type"] = "application/json";
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}
export async function adminPhotoExhibitionSubmissions(eventId:number):Promise<PhotoExhibitionAdminSubmission[]>{const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/submissions`,{headers:authHeaders()});return handle(r);}
export async function adminPhotoExhibitionPreview(eventId:number):Promise<PhotoExhibitionPreview>{const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/preview`,{headers:authHeaders()});return handle(r);}
export async function adminExcludePhotoExhibition(eventId:number,submissionId:number,reason:string):Promise<PhotoExhibitionAdminSubmission>{const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/submissions/${submissionId}/exclude`,{method:"PATCH",headers:authHeaders(),body:JSON.stringify({reason})});return handle(r);}
export async function adminFinalizePhotoExhibition(eventId:number):Promise<void>{const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/finalize`,{method:"POST",headers:authHeaders()});return handle(r);}
export async function adminStartPhotoExhibitionVoting(eventId:number):Promise<void>{
  const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/start-voting`,{method:"POST",headers:authHeaders()});
  if(!r.ok) return handle(r);
}
export async function adminPhotoExhibitionVoterAudit(eventId:number):Promise<PhotoExhibitionVoterAudit[]>{const r=await fetch(`${BASE_URL}/admin/events/${eventId}/photo-exhibition/audit-voters`,{headers:authHeaders()});return handle(r);}

export async function getMyPhotoExhibitionSubmission(eventId: number): Promise<PhotoExhibitionSubmission> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/my-submission`, { cache: "no-store", headers: authHeaders() });
  return handle(res);
}
export async function savePhotoExhibitionSubmission(eventId: number, title: string, introduction: string): Promise<PhotoExhibitionSubmission> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/my-submission`, { method: "PATCH", headers: authHeaders(), body: JSON.stringify({ title, introduction }) });
  return handle(res);
}
export async function deleteMyPhotoExhibitionSubmission(eventId: number, submissionId?: number): Promise<void> {
  const qs = submissionId ? `?submissionId=${submissionId}` : "";
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/my-submission${qs}`, { method: "DELETE", headers: authHeaders() });
  if (!res.ok) return handle(res);
}
export async function uploadPhotoExhibitionImage(eventId: number, file: File): Promise<PhotoExhibitionSubmission> {
  const data = new FormData(); data.append("image", file); const token = getToken();
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/images`, { method: "POST", headers: token ? { Authorization: `Bearer ${token}` } : {}, body: data });
  return handle(res);
}
export async function deletePhotoExhibitionImage(eventId: number, imageId: number): Promise<PhotoExhibitionSubmission> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/images/${imageId}`, { method: "DELETE", headers: authHeaders() }); return handle(res);
}
export async function orderPhotoExhibitionImages(eventId: number, imageIds: number[]): Promise<PhotoExhibitionSubmission> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/images/order`, { method: "PUT", headers: authHeaders(), body: JSON.stringify({ imageIds }) }); return handle(res);
}
export async function getPhotoExhibitionGallery(eventId: number): Promise<PhotoExhibitionSubmission[]> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/gallery`, { cache: "no-store", headers: authHeaders() }); return handle(res);
}
export async function savePhotoExhibitionVotes(eventId: number, submissionIds: number[]): Promise<number[]> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/votes`, { method: "PUT", headers: authHeaders(), body: JSON.stringify({ submissionIds }) }); return handle(res);
}
export async function getMyPhotoExhibitionVotes(eventId: number): Promise<number[]> {
  const res = await fetch(`${BASE_URL}/events/${eventId}/photo-exhibition/my-votes`, { cache: "no-store", headers: authHeaders() }); return handle(res);
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
