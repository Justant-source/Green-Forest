/** GUI 표시는 항상 KST. API의 timezone 없는 LocalDateTime은 UTC instant로 해석한다. */

export const KST_TZ = "Asia/Seoul";

function hasOffset(s: string): boolean {
  return /Z$|[+-]\d{2}:\d{2}$/.test(s);
}

function toIsoCandidate(raw: string): string {
  const s = raw.trim();
  if (!s) return s;
  return s.includes("T") ? s : s.replace(" ", "T");
}

/** JDBC/Jackson이 내려주는 naive ISO를 UTC instant로 파싱한다. */
export function parseServerInstant(value: string | Date | null | undefined): Date | null {
  if (value == null || value === "") return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
  const iso = toIsoCandidate(value);
  if (!iso) return null;
  const d = new Date(hasOffset(iso) ? iso : `${iso}Z`);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** EventMode serverNow처럼 Java에서 KST 벽시계로 만든 naive ISO. */
export function parseKstWall(value: string | Date | null | undefined): Date | null {
  if (value == null || value === "") return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
  const iso = toIsoCandidate(value);
  if (!iso) return null;
  const d = new Date(hasOffset(iso) ? iso : `${iso}+09:00`);
  return Number.isNaN(d.getTime()) ? null : d;
}

const KST_DT: Intl.DateTimeFormatOptions = {
  timeZone: KST_TZ,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
};

export function formatKstDateTime(value: string | Date | null | undefined): string {
  const d = parseServerInstant(value);
  if (!d) return "";
  return new Intl.DateTimeFormat("sv-SE", KST_DT).format(d).replace("T", " ");
}

export function formatKstDate(value: string | Date | null | undefined): string {
  const d = parseServerInstant(value);
  if (!d) return "";
  return d.toLocaleDateString("ko-KR", { timeZone: KST_TZ });
}

export function formatKstTime(value: string | Date | null | undefined): string {
  const d = parseServerInstant(value);
  if (!d) return "";
  return d.toLocaleTimeString("ko-KR", {
    timeZone: KST_TZ,
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

export function formatKstRelative(value: string | Date | null | undefined): string {
  const d = parseServerInstant(value);
  if (!d) return "";
  const diffMs = Date.now() - d.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);
  if (diffMin < 1) return "방금 전";
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  if (diffDay < 30) return `${diffDay}일 전`;
  return formatKstDate(d);
}

export function todayKstDateString(): string {
  return new Date().toLocaleDateString("en-CA", { timeZone: KST_TZ });
}

export function currentKstYearMonth(): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: KST_TZ,
    year: "numeric",
    month: "2-digit",
  }).formatToParts(new Date());
  const y = parts.find((p) => p.type === "year")?.value;
  const m = parts.find((p) => p.type === "month")?.value;
  return `${y}-${m}`;
}

/** datetime-local 입력용 (KST 벽시계). */
export function toDatetimeLocalKst(value: string | Date | null | undefined): string {
  const d = parseServerInstant(value);
  if (!d) return "";
  return new Intl.DateTimeFormat("sv-SE", KST_DT).format(d).replace(" ", "T").slice(0, 16);
}
