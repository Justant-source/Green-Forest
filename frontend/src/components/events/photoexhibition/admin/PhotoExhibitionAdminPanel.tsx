"use client";

import { useEffect, useState } from "react";
import { toMediaUrl } from "@/lib/api";
import {
  adminExcludePhotoExhibition,
  adminFinalizePhotoExhibition,
  adminPhotoExhibitionPreview,
  adminPhotoExhibitionSubmissions,
  adminPhotoExhibitionVoterAudit,
} from "@/lib/events/api";
import type {
  Event,
  PhotoExhibitionAdminSubmission,
  PhotoExhibitionPreview,
  PhotoExhibitionVoterAudit,
} from "@/lib/events/types";

export default function PhotoExhibitionAdminPanel({
  event,
  onBack,
}: {
  event: Event;
  onBack: () => void;
}) {
  const [submissions, setSubmissions] = useState<PhotoExhibitionAdminSubmission[]>([]);
  const [preview, setPreview] = useState<PhotoExhibitionPreview | null>(null);
  const [audit, setAudit] = useState<PhotoExhibitionVoterAudit[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const [s, p, a] = await Promise.all([
        adminPhotoExhibitionSubmissions(event.id),
        adminPhotoExhibitionPreview(event.id),
        adminPhotoExhibitionVoterAudit(event.id),
      ]);
      setSubmissions(s);
      setPreview(p);
      setAudit(a);
    } catch (e: any) {
      setError(e.message || "불러오기 실패");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [event.id]);

  const canExclude = ["SCHEDULED", "SUBMISSION", "REVIEW"].includes(event.phase || "");
  const canFinalize = event.phase === "TALLY_PENDING" || event.status === "ENDED";
  const auditByVoter = audit.reduce<Record<string, string[]>>((all, row) => {
    (all[row.voterNickname] ||= []).push(row.workTitle);
    return all;
  }, {});

  const exclude = async (id: number) => {
    const reason = prompt("제외 사유를 입력하세요.");
    if (!reason?.trim()) return;
    setBusy(true);
    try {
      await adminExcludePhotoExhibition(event.id, id, reason);
      await load();
    } catch (e: any) {
      setError(e.message || "제외 실패");
    } finally {
      setBusy(false);
    }
  };

  const finalize = async () => {
    if (!preview || !confirm(`예상 ${preview.grandTotal}💧 지급을 확정할까요?`)) return;
    setBusy(true);
    try {
      await adminFinalizePhotoExhibition(event.id);
      await load();
    } catch (e: any) {
      setError(e.message || "확정 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="space-y-4">
      <button onClick={onBack}>← 이벤트 목록</button>
      <h2 className="font-bold">
        {event.title} · {event.phase || event.status}
      </h2>
      {error && <p className="text-red-600">{error}</p>}
      {loading ? (
        <p>불러오는 중…</p>
      ) : (
        <>
          <section className="rounded border p-3">
            <h3>예상 수상</h3>
            {preview?.candidates.map((c) => (
              <p key={c.submissionId}>
                {c.authorNickname} · {c.title} · {c.voteCount}표 · {c.proposedTier || "미수상"} · {c.reward}💧
              </p>
            ))}
            <p>총 예상 {preview?.grandTotal}💧</p>
          </section>
          <section className="rounded border p-3">
            <h3>투표 감사</h3>
            {Object.entries(auditByVoter).map(([v, works]) => (
              <p key={v}>
                {v}: {works.join(", ")}
              </p>
            ))}
          </section>
          <section>
            {submissions.map((s) => (
              <article key={s.id} className="rounded border p-3">
                <b>{s.title}</b> · {s.authorNickname} · {s.voteCount}표
                <div className="flex gap-2">
                  {s.images.map((i) => (
                    <img
                      key={i.id}
                      src={toMediaUrl(i.imageUrl, "sm")}
                      alt=""
                      className="h-14 w-14 object-cover"
                    />
                  ))}
                  {canExclude && !s.excluded && (
                    <button disabled={busy} onClick={() => exclude(s.id)}>
                      제외
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>
          {canFinalize && event.status !== "SCORED" && (
            <button disabled={busy} onClick={finalize}>
              최종 결과 확정
            </button>
          )}
        </>
      )}
    </section>
  );
}
