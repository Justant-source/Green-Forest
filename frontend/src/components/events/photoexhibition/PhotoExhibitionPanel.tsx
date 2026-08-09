"use client";

import { ChangeEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toMediaUrl } from "@/lib/api";
import { compressBingoImage } from "@/lib/events/imageCompression";
import {
  deleteMyPhotoExhibitionSubmission,
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
  const router = useRouter();
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

  const removeSubmission = async () => {
    if (!submission) return;
    if (!confirm("출품작을 삭제할까요? 광장 게시글과 사진이 함께 삭제됩니다.")) return;
    setSaving(true);
    setError("");
    try {
      await deleteMyPhotoExhibitionSubmission(eventId);
      sessionStorage.removeItem("gridFeedCache");
      setSubmission(null);
      setTitle("");
      setIntroduction("");
      setGallery((prev) => prev.filter((w) => !w.mine));
      router.push("/");
    } catch (e: any) {
      setError(e.message || "출품작을 삭제하지 못했습니다.");
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
          <p className="mt-3 text-xs text-gray-500">사진을 누르면 대표 사진으로 지정됩니다.</p>
          <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
            {submission?.images.map((image) => (
              <div key={image.id} className="relative overflow-hidden rounded-lg border bg-gray-50">
                <button
                  type="button"
                  onClick={() => {
                    if (!image.representative && !saving) promote(image.id);
                  }}
                  disabled={image.representative || saving}
                  className="group relative block w-full text-left disabled:cursor-default"
                  title={image.representative ? "대표 사진" : "대표로 지정"}
                >
                  <img
                    src={toMediaUrl(image.imageUrl, "sm")}
                    alt=""
                    className="aspect-square w-full object-cover"
                  />
                  {image.representative ? (
                    <span className="absolute left-1.5 top-1.5 inline-flex items-center gap-1 rounded-full bg-forest-600 px-2 py-0.5 text-[11px] font-semibold text-white shadow">
                      <svg viewBox="0 0 20 20" fill="currentColor" className="h-3 w-3" aria-hidden>
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                      대표
                    </span>
                  ) : (
                    <span className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-1 bg-black/55 py-1.5 text-[11px] font-medium text-white opacity-90 transition group-hover:bg-black/70 group-hover:opacity-100">
                      <svg viewBox="0 0 20 20" fill="currentColor" className="h-3.5 w-3.5" aria-hidden>
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                      대표로
                    </span>
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => remove(image.id)}
                  disabled={saving}
                  className="absolute right-1.5 top-1.5 inline-flex items-center gap-0.5 rounded-full bg-white/95 px-1.5 py-0.5 text-[11px] font-medium text-red-600 shadow hover:bg-red-50 disabled:opacity-50"
                  title="삭제"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor" className="h-3 w-3" aria-hidden>
                    <path
                      fillRule="evenodd"
                      d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193v-.443A2.75 2.75 0 0011.25 1h-2.5zM10 4c.784 0 1.556.022 2.31.066v.43a41.56 41.56 0 00-4.62 0v-.43A41.56 41.56 0 0110 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z"
                      clipRule="evenodd"
                    />
                  </svg>
                  삭제
                </button>
              </div>
            ))}
            {(submission?.images.length || 0) < 4 && (
              <label className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-gray-300 text-sm text-gray-500 hover:border-forest-400 hover:bg-forest-50 hover:text-forest-700">
                <svg viewBox="0 0 20 20" fill="currentColor" className="h-6 w-6" aria-hidden>
                  <path d="M10.75 4.75a.75.75 0 00-1.5 0v4.5h-4.5a.75.75 0 000 1.5h4.5v4.5a.75.75 0 001.5 0v-4.5h4.5a.75.75 0 000-1.5h-4.5v-4.5z" />
                </svg>
                사진 추가
                <input type="file" accept="image/*" className="hidden" onChange={upload} />
              </label>
            )}
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <button
              onClick={save}
              disabled={saving}
              className="rounded bg-forest-600 px-3 py-2 text-white disabled:opacity-50"
            >
              저장
            </button>
            {submission && (submission.title || submission.images.length > 0) && (
              <button
                type="button"
                onClick={removeSubmission}
                disabled={saving}
                className="inline-flex items-center gap-1 rounded border border-red-300 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
              >
                <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
                  <path
                    fillRule="evenodd"
                    d="M8.75 1A2.75 2.75 0 006 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 10.23 1.482l.149-.022.841 10.518A2.75 2.75 0 007.596 19h4.807a2.75 2.75 0 002.742-2.53l.841-10.52.149.023a.75.75 0 00.23-1.482A41.03 41.03 0 0014 4.193v-.443A2.75 2.75 0 0011.25 1h-2.5zM10 4c.784 0 1.556.022 2.31.066v.43a41.56 41.56 0 00-4.62 0v-.43A41.56 41.56 0 0110 4zM8.58 7.72a.75.75 0 00-1.5.06l.3 7.5a.75.75 0 101.5-.06l-.3-7.5zm4.34.06a.75.75 0 10-1.5-.06l-.3 7.5a.75.75 0 101.5.06l.3-7.5z"
                    clipRule="evenodd"
                  />
                </svg>
                출품 삭제
              </button>
            )}
          </div>
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
