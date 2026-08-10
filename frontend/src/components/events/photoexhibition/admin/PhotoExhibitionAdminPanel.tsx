"use client";

import { useEffect, useState } from "react";
import { toMediaUrl } from "@/lib/api";
import {
  adminExcludePhotoExhibition,
  adminFinalizePhotoExhibition,
  adminPhotoExhibitionPreview,
  adminPhotoExhibitionSubmissions,
  adminPhotoExhibitionVoterAudit,
  adminStartPhotoExhibitionVoting,
  getEvent,
} from "@/lib/events/api";
import type {
  Event,
  PhotoExhibitionAdminSubmission,
  PhotoExhibitionPreview,
  PhotoExhibitionVoterAudit,
} from "@/lib/events/types";

export default function PhotoExhibitionAdminPanel({
  event: initial,
  onBack,
}: {
  event: Event;
  onBack: () => void;
}) {
  const [event, setEvent] = useState(initial);
  const [submissions, setSubmissions] = useState<PhotoExhibitionAdminSubmission[]>([]);
  const [preview, setPreview] = useState<PhotoExhibitionPreview | null>(null);
  const [audit, setAudit] = useState<PhotoExhibitionVoterAudit[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const [fresh, s, p, a] = await Promise.all([
        getEvent(initial.id),
        adminPhotoExhibitionSubmissions(initial.id),
        adminPhotoExhibitionPreview(initial.id),
        adminPhotoExhibitionVoterAudit(initial.id),
      ]);
      setEvent(fresh);
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
  }, [initial.id]);

  const canExclude = ["SCHEDULED", "SUBMISSION", "REVIEW", "VOTING"].includes(event.phase || "");
  const canStartVoting = event.phase === "REVIEW" && !event.photoExhibitionConfig?.votingStartedAt;
  const canFinalize = event.phase === "TALLY_PENDING" || event.status === "ENDED";
  const auditByVoter = audit.reduce<Record<string, string[]>>((all, row) => {
    (all[row.voterNickname] ||= []).push(row.workTitle);
    return all;
  }, {});

  const exclude = async (id: number) => {
    const reason = prompt("제외 사유를 입력하세요. (해당 작품 표는 삭제됩니다)");
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

  const startVoting = async () => {
    if (!confirm("투표를 지금 공개할까요? 배너에 투표 안내가 노출됩니다.")) return;
    setBusy(true);
    try {
      await adminStartPhotoExhibitionVoting(event.id);
      await load();
    } catch (e: any) {
      setError(e.message || "투표 시작 실패");
    } finally {
      setBusy(false);
    }
  };

  const finalize = async () => {
    if (
      !preview ||
      !confirm(
        `예상 ${preview.grandTotal}💧 지급을 확정할까요?\n(참가 100 · 투표 10/표 · 1등 500 · 2등 300 · 3등 CA 물개박수)\n수상 공지는 직접 올려 주세요.`,
      )
    )
      return;
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
      <button type="button" onClick={onBack}>
        ← 이벤트 목록
      </button>
      <h2 className="font-bold">
        {event.title} · {event.phase || event.status}
      </h2>
      <p className="text-xs text-gray-500">
        보상: 참가 100💧 · 투표 10/20/30💧 · 1등 500💧 · 2등 300💧 · 3등 CA 물개박수 · 수상 공지는 수동
      </p>
      {error && <p className="text-red-600">{error}</p>}
      {loading ? (
        <p>불러오는 중…</p>
      ) : (
        <>
          {canStartVoting && (
            <button
              type="button"
              disabled={busy}
              onClick={startVoting}
              className="rounded-lg bg-forest-600 px-4 py-2 text-white"
            >
              투표 시작 (공개)
            </button>
          )}
          <section className="rounded border p-3">
            <h3 className="font-medium">예상 수상</h3>
            {preview?.candidates.map((c) => (
              <p key={c.submissionId}>
                {c.authorNickname} · {c.title} · {c.voteCount}표 · {c.proposedTier || "미수상"} · {c.reward}
                💧
              </p>
            ))}
            <p className="mt-1 text-sm">총 예상 {preview?.grandTotal}💧</p>
          </section>
          <section className="rounded border p-3">
            <h3 className="font-medium">투표 감사</h3>
            {Object.entries(auditByVoter).map(([v, works]) => (
              <p key={v}>
                {v}: {works.join(", ")}
              </p>
            ))}
          </section>
          <section className="space-y-2">
            {submissions.map((s) => (
              <article key={s.id} className="rounded border p-3">
                <b>{s.title || "(제목 없음)"}</b> · {s.authorNickname} · {s.voteCount}표
                {s.excluded && <span className="ml-2 text-red-600">제외됨</span>}
                <div className="mt-2 flex flex-wrap gap-2">
                  {s.images.map((i) => (
                    <img
                      key={i.id}
                      src={toMediaUrl(i.imageUrl, "sm")}
                      alt=""
                      className="h-14 w-14 object-cover"
                    />
                  ))}
                  {canExclude && !s.excluded && (
                    <button type="button" disabled={busy} onClick={() => exclude(s.id)} className="text-sm text-red-600">
                      제외
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>
          {canFinalize && event.status !== "SCORED" && (
            <button type="button" disabled={busy} onClick={finalize} className="rounded-lg border border-forest-600 px-4 py-2 text-forest-700">
              최종 결과 확정 (보상 지급)
            </button>
          )}
        </>
      )}
    </section>
  );
}
