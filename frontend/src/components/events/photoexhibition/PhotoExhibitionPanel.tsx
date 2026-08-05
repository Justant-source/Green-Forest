"use client";

import { ChangeEvent, useEffect, useState } from "react";
import { toMediaUrl } from "@/lib/api";
import { compressBingoImage } from "@/lib/events/imageCompression";
import {
  deletePhotoExhibitionImage,
  getMyPhotoExhibitionSubmission,
  orderPhotoExhibitionImages,
  getPhotoExhibitionGallery,
  getMyPhotoExhibitionVotes,
  savePhotoExhibitionSubmission,
  savePhotoExhibitionVotes,
  uploadPhotoExhibitionImage,
} from "@/lib/events/api";
import type {
  PhotoExhibitionConfig,
  PhotoExhibitionSubmission,
} from "@/lib/events/types";

interface Props {
  eventId: number;
  status: string;
  phase?: string;
  config?: PhotoExhibitionConfig | null;
  serverNow?: string;
}

export default function PhotoExhibitionPanel({
  eventId,
  phase,
  config,
  serverNow,
}: Props) {
  const editable = phase === "SUBMISSION";
  const [submission, setSubmission] = useState<PhotoExhibitionSubmission | null>(null);
  const [title, setTitle] = useState("");
  const [introduction, setIntroduction] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [gallery, setGallery] = useState<PhotoExhibitionSubmission[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [detail, setDetail] = useState<PhotoExhibitionSubmission | null>(null);

  const apply = (next: PhotoExhibitionSubmission) => {
    setSubmission(next);
    setTitle(next.title || "");
    setIntroduction(next.introduction || "");
  };

  useEffect(() => {
    const load = async () => {
      try {
        const works = await getPhotoExhibitionGallery(eventId);
        setGallery(works);
        if (editable) apply(await getMyPhotoExhibitionSubmission(eventId));
        if (phase === "VOTING") setSelected(await getMyPhotoExhibitionVotes(eventId));
      } catch (e: any) {
        setError(e.message || "전시회를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [eventId, editable, phase]);

  const save = async () => {
    if (!title.trim() || !introduction.trim()) {
      setError("제목과 작품 소개를 모두 입력하세요.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      apply(await savePhotoExhibitionSubmission(eventId, title.trim(), introduction.trim()));
    } catch (e: any) {
      setError(e.message || "저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !submission || submission.images.length >= 4) return;
    setSaving(true);
    setError("");
    try {
      apply(await uploadPhotoExhibitionImage(eventId, await compressBingoImage(file)));
    } catch (e: any) {
      setError(e.message || "사진을 올리지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const remove = async (imageId: number) => {
    setSaving(true);
    try {
      apply(await deletePhotoExhibitionImage(eventId, imageId));
    } catch (e: any) {
      setError(e.message || "사진을 삭제하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const promote = async (imageId: number) => {
    if (!submission) return;
    const ids = [
      imageId,
      ...submission.images.filter((x) => x.id !== imageId).map((x) => x.id),
    ];
    setSaving(true);
    try {
      apply(await orderPhotoExhibitionImages(eventId, ids));
    } catch (e: any) {
      setError(e.message || "대표 사진을 변경하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const voting = phase === "VOTING";
  const result = phase === "RESULT";
  const tierLabel: Record<string, string> = {
    FIRST: "1등",
    SECOND: "2등",
    THIRD: "3등 · CA 물개박수",
  };
  const toggle = (id: number) =>
    setSelected((old) =>
      old.includes(id)
        ? old.filter((value) => value !== id)
        : old.length === 3
          ? old
          : [...old, id],
    );

  const saveVotes = async () => {
    setSaving(true);
    try {
      setSelected(await savePhotoExhibitionVotes(eventId, selected));
      setError("투표 선택을 저장했습니다.");
    } catch (e: any) {
      setError(e.message || "투표를 저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="py-8 text-center text-sm text-gray-500">내 작품을 불러오는 중…</div>;
  }

  const cancelVotes = async () => {
    setSaving(true);
    try {
      setSelected(await savePhotoExhibitionVotes(eventId, []));
      setError("투표 선택을 모두 취소했습니다.");
    } catch (e: any) {
      setError(e.message || "투표 취소에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="space-y-4">
      <div className="rounded bg-forest-50 p-3 text-sm text-forest-800">
        {
          ({
            SUBMISSION: "작품 제출",
            REVIEW: "검토",
            VOTING: "투표",
            TALLY_PENDING: "집계 대기",
            RESULT: "결과",
          } as Record<string, string>)[phase || ""] || "이벤트"
        }
        {config && <span className="ml-2 text-xs">마감 {config.votingEnd} KST</span>}
        {serverNow && <span className="ml-2 text-xs">현재 {serverNow}</span>}
      </div>

      {editable && (
        <div className="rounded-xl border bg-white p-4">
          <h2 className="font-bold">내 출품작</h2>
          <input
            value={title}
            maxLength={150}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="작품 제목"
            className="mt-3 w-full rounded border p-2"
          />
          <textarea
            value={introduction}
            maxLength={2000}
            onChange={(e) => setIntroduction(e.target.value)}
            placeholder="작품 소개"
            className="mt-2 w-full rounded border p-2"
          />
          <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
            {submission?.images.map((image) => (
              <div key={image.id}>
                <img
                  src={toMediaUrl(image.imageUrl, "sm")}
                  alt=""
                  className="aspect-square w-full object-cover"
                />
                <button
                  onClick={() => promote(image.id)}
                  disabled={image.representative}
                  className="text-xs"
                >
                  대표로
                </button>
                <button onClick={() => remove(image.id)} className="ml-2 text-xs text-red-600">
                  삭제
                </button>
              </div>
            ))}
            {(submission?.images.length || 0) < 4 && (
              <label className="flex aspect-square items-center justify-center border">
                사진 추가
                <input type="file" className="hidden" onChange={upload} />
              </label>
            )}
          </div>
          <button onClick={save} className="mt-3 rounded bg-forest-600 px-3 py-2 text-white">
            저장
          </button>
        </div>
      )}

      <h2 className="font-bold">전시 갤러리</h2>
      {error && <p className="text-sm text-forest-700">{error}</p>}
      <div className="grid gap-4 sm:grid-cols-2">
        {gallery.map((work) => (
          <article key={work.id} className="overflow-hidden rounded-xl border">
            <button onClick={() => setDetail(work)} className="block w-full text-left">
              <img
                src={toMediaUrl(work.images[0]?.imageUrl, "md")}
                alt=""
                className="aspect-video w-full object-cover"
              />
              <div className="p-3">
                <b>{work.title}</b>
                <p>{result ? work.authorNickname : "익명 출품"}</p>
                {result && (
                  <p>
                    {work.resultTier ? tierLabel[work.resultTier] || work.resultTier : "미수상"} · {work.finalVotes}표
                  </p>
                )}
              </div>
            </button>
            {voting && (
              <button
                disabled={work.mine}
                onClick={() => toggle(work.id)}
                className="m-2 rounded border px-2"
              >
                {work.mine ? "내 작품" : selected.includes(work.id) ? "선택됨" : "선택"}
              </button>
            )}
          </article>
        ))}
      </div>

      {voting && (
        <div className="sticky bottom-2 rounded bg-forest-700 p-3 text-white">
          {selected.length}/3 · {selected.length * 10}💧 <button onClick={saveVotes}>저장</button>
          <button onClick={cancelVotes}>전체 취소</button>
        </div>
      )}

      {detail && (
        <div className="fixed inset-0 z-50 bg-black/60 p-4">
          <div className="mx-auto max-w-2xl bg-white p-4">
            <img src={toMediaUrl(detail.images[0]?.imageUrl, "md")} alt="" />
            <div className="mt-3 grid w-2/3 grid-cols-3 gap-2">
              {detail.images.slice(1).map((image) => (
                <img key={image.id} src={toMediaUrl(image.imageUrl, "sm")} alt="" />
              ))}
            </div>
            <h3>{detail.title} 상세</h3>
            <p>{detail.introduction}</p>
            <button onClick={() => setDetail(null)}>닫기</button>
          </div>
        </div>
      )}
    </section>
  );
}
