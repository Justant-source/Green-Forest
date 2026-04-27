"use client";

import { useEffect, useMemo, useState } from "react";
import { adminListSubmissions, adminScoreCell } from "@/lib/events/api";
import { toMediaUrl } from "@/lib/api";
import { countCompletedLines } from "@/lib/events/bingo-rules";
import type { PhotoBingoSubmissionDto } from "@/lib/events/types";

interface Props {
  eventId: number;
  onBack: () => void;
}

export default function BingoScoringPanel({ eventId, onBack }: Props) {
  const [submissions, setSubmissions] = useState<PhotoBingoSubmissionDto[] | null>(null);
  const [selectedIdx, setSelectedIdx] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = async () => {
    try {
      const list = await adminListSubmissions(eventId);
      setSubmissions(list);
    } catch (e: any) {
      setError(e?.message ?? "로딩 실패");
    }
  };

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId]);

  const current = submissions?.[selectedIdx] ?? null;

  const scoredCount = useMemo(() => {
    if (!submissions) return 0;
    return submissions.filter((s) =>
      s.cells.every((c) => c.imageUrl === null || c.scoreStatus !== "PENDING")
    ).length;
  }, [submissions]);

  const approvedSet = useMemo(() => {
    const s = new Set<number>();
    current?.cells.forEach((c) => {
      if (c.scoreStatus === "APPROVED") s.add(c.cellIndex);
    });
    return s;
  }, [current]);

  const score = async (cellId: number, status: "APPROVED" | "REJECTED") => {
    if (!current) return;
    setBusy(true);
    try {
      const updated = await adminScoreCell(eventId, cellId, status);
      setSubmissions((prev) =>
        prev?.map((s) => (s.submissionId === updated.submissionId ? updated : s)) ?? null
      );
    } catch (e: any) {
      setError(e?.message ?? "채점 실패");
    } finally {
      setBusy(false);
    }
  };

  const approveAll = async () => {
    if (!current) return;
    if (!confirm("이 빙고판의 업로드된 셀을 모두 승인할까요?")) return;
    setBusy(true);
    try {
      for (const c of current.cells) {
        if (c.imageUrl && c.scoreStatus !== "APPROVED") {
          await adminScoreCell(eventId, c.id, "APPROVED");
        }
      }
      await refresh();
    } catch (e: any) {
      setError(e?.message ?? "일괄 승인 실패");
    } finally {
      setBusy(false);
    }
  };

  if (error) {
    return (
      <div className="space-y-3">
        <button onClick={onBack} className="text-sm text-gray-600 hover:text-gray-800">
          ← 목록
        </button>
        <div className="text-red-500">{error}</div>
      </div>
    );
  }
  if (!submissions) {
    return (
      <div className="flex justify-center py-10">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }
  if (submissions.length === 0) {
    return (
      <div className="space-y-3">
        <button onClick={onBack} className="text-sm text-gray-600 hover:text-gray-800">
          ← 목록
        </button>
        <div className="text-center text-gray-500 py-10">참여자가 없습니다.</div>
      </div>
    );
  }

  const lines = countCompletedLines(approvedSet);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <button onClick={onBack} className="text-sm text-gray-600 hover:text-gray-800">
          ← 목록
        </button>
        <div className="text-xs text-gray-500">
          채점 완료: <b>{scoredCount}/{submissions.length}</b>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={() => setSelectedIdx((i) => Math.max(0, i - 1))}
          disabled={selectedIdx === 0}
          className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 text-sm disabled:opacity-50"
        >
          ◀
        </button>
        <select
          value={selectedIdx}
          onChange={(e) => setSelectedIdx(Number(e.target.value))}
          className="flex-1 border border-gray-200 rounded-lg px-2 py-1.5 text-sm"
        >
          {submissions.map((s, i) => {
            const uploadedN = s.cells.filter((c) => c.imageUrl).length;
            const pendingN = s.cells.filter((c) => c.imageUrl && c.scoreStatus === "PENDING").length;
            return (
              <option key={s.submissionId} value={i}>
                {s.userNickname} — 업로드 {uploadedN}/9 · 미채점 {pendingN}
              </option>
            );
          })}
        </select>
        <button
          onClick={() => setSelectedIdx((i) => Math.min(submissions.length - 1, i + 1))}
          disabled={selectedIdx >= submissions.length - 1}
          className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 text-sm disabled:opacity-50"
        >
          ▶
        </button>
      </div>

      {current && (
        <>
          {current.caption && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-2 text-xs text-gray-700">
              {current.caption}
            </div>
          )}
          <div className="text-xs text-gray-600">
            승인 기준 {approvedSet.size}/9 · <b>{lines}줄</b> 달성
          </div>
          <div className="grid grid-cols-3 gap-2">
            {current.cells.map((c) => (
              <div key={c.id} className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                <div className="aspect-square bg-gray-100 relative">
                  {c.imageUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={toMediaUrl(c.imageUrl, "md")} alt={c.theme} loading="lazy" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-[10px] text-gray-400 text-center p-1">
                      (미업로드)<br />{c.theme}
                    </div>
                  )}
                  {c.imageUrl && (
                    <div className="absolute inset-x-0 bottom-0 bg-black/40 text-white text-[9px] p-0.5 leading-tight text-center break-keep">
                      {c.theme}
                    </div>
                  )}
                </div>
                <div className="p-1.5 flex gap-1">
                  <button
                    disabled={busy || !c.imageUrl}
                    onClick={() => score(c.id, "APPROVED")}
                    className={`flex-1 py-1 rounded text-xs font-medium ${
                      c.scoreStatus === "APPROVED"
                        ? "bg-green-500 text-white"
                        : "bg-gray-100 text-gray-700 hover:bg-green-100"
                    } disabled:opacity-40`}
                  >
                    ✅
                  </button>
                  <button
                    disabled={busy || !c.imageUrl}
                    onClick={() => score(c.id, "REJECTED")}
                    className={`flex-1 py-1 rounded text-xs font-medium ${
                      c.scoreStatus === "REJECTED"
                        ? "bg-red-500 text-white"
                        : "bg-gray-100 text-gray-700 hover:bg-red-100"
                    } disabled:opacity-40`}
                  >
                    ❌
                  </button>
                </div>
              </div>
            ))}
          </div>
          <div className="flex gap-2">
            <button
              onClick={approveAll}
              disabled={busy}
              className="flex-1 py-2 rounded-full bg-green-600 hover:bg-green-700 text-white text-sm font-medium disabled:opacity-60"
            >
              이 판 모두 승인
            </button>
            <button
              onClick={() => setSelectedIdx((i) => Math.min(submissions.length - 1, i + 1))}
              disabled={selectedIdx >= submissions.length - 1}
              className="flex-1 py-2 rounded-full bg-forest-500 hover:bg-forest-600 text-white text-sm font-medium disabled:opacity-60"
            >
              다음 참여자 →
            </button>
          </div>
        </>
      )}
    </div>
  );
}
