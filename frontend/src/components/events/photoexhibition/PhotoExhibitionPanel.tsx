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

type Tab = "mine" | "others";

function unescapeNewlines(text: string | null | undefined): string {
  return (text || "").replace(/\\n/g, "\n");
}

export default function PhotoExhibitionPanel({ eventId, phase }: Props) {
  const router = useRouter();
  const editable = phase === "SUBMISSION";
  const voting = phase === "VOTING";
  const locked = phase === "REVIEW";
  const closed = phase === "TALLY_PENDING" || phase === "RESULT" || phase === "SCHEDULED";

  const [tab, setTab] = useState<Tab>("mine");
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

  const applyImages = (next: PhotoExhibitionSubmission) => {
    setSubmission(next);
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        if (closed || locked) {
          setGallery([]);
          setSubmission(null);
          return;
        }
        if (editable) {
          apply(await getMyPhotoExhibitionSubmission(eventId));
          setGallery(await getPhotoExhibitionGallery(eventId));
          setTab("mine");
        } else if (voting) {
          setGallery(await getPhotoExhibitionGallery(eventId));
          setSelected(await getMyPhotoExhibitionVotes(eventId));
        }
      } catch (e: any) {
        setError(e.message || "전시회를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [eventId, editable, voting, locked, closed, phase]);

  const save = async () => {
    if (!title.trim() || !introduction.trim()) {
      setError("제목과 작품 소개는 필수입니다.");
      return;
    }
    if (!submission?.images.length) {
      setError("사진은 최소 1장 필요합니다.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      apply(await savePhotoExhibitionSubmission(eventId, title.trim(), introduction.trim()));
      sessionStorage.removeItem("gridFeedCache");
      router.push("/");
    } catch (e: any) {
      setError(e.message || "저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const persistDraftIfReady = async () => {
    if (!title.trim() || !introduction.trim()) return;
    await savePhotoExhibitionSubmission(eventId, title.trim(), introduction.trim());
  };

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !submission || submission.images.length >= 4) return;
    setSaving(true);
    setError("");
    try {
      await persistDraftIfReady();
      applyImages(await uploadPhotoExhibitionImage(eventId, await compressBingoImage(file)));
      setGallery(await getPhotoExhibitionGallery(eventId));
    } catch (e: any) {
      setError(e.message || "사진을 올리지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const remove = async (imageId: number) => {
    setSaving(true);
    try {
      applyImages(await deletePhotoExhibitionImage(eventId, imageId));
      setGallery(await getPhotoExhibitionGallery(eventId));
    } catch (e: any) {
      setError(e.message || "사진을 삭제하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const promote = async (imageId: number) => {
    if (!submission) return;
    const ids = [imageId, ...submission.images.filter((x) => x.id !== imageId).map((x) => x.id)];
    setSaving(true);
    try {
      applyImages(await orderPhotoExhibitionImages(eventId, ids));
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

  const toggle = (id: number) =>
    setSelected((old) =>
      old.includes(id) ? old.filter((value) => value !== id) : old.length === 3 ? old : [...old, id],
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

  if (loading) {
    return <div className="py-8 text-center text-sm text-gray-500">불러오는 중…</div>;
  }

  if (locked) {
    return (
      <div className="rounded-xl border bg-white p-8 text-center">
        <p className="font-semibold text-gray-900">출품 마감 · 투표 준비 중</p>
        <p className="mt-2 text-sm text-gray-600">투표가 시작되면 배너로 안내됩니다.</p>
      </div>
    );
  }

  if (closed) {
    return (
      <div className="rounded-xl border bg-white p-8 text-center">
        <p className="font-semibold text-gray-900">이벤트가 종료되었습니다</p>
        <p className="mt-2 text-sm text-gray-600">결과는 공지를 확인해 주세요.</p>
      </div>
    );
  }

  const others = gallery.filter((w) => !w.mine);
  const starIcon = (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-3.5 w-3.5" aria-hidden>
      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
    </svg>
  );

  const detailModal = detail && (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl bg-white p-4">
        <div className="flex justify-center rounded-lg bg-gray-100">
          <img
            src={toMediaUrl(detail.images[0]?.imageUrl, "md")}
            alt=""
            className="max-h-[70vh] w-auto max-w-full rounded-lg object-contain"
          />
        </div>
        {detail.images.length > 1 && (
          <div className="mt-3 grid grid-cols-3 gap-2">
            {detail.images.slice(1).map((image) => (
              <img
                key={image.id}
                src={toMediaUrl(image.imageUrl, "sm")}
                alt=""
                className="aspect-square w-full rounded-lg object-cover"
              />
            ))}
          </div>
        )}
        <h3 className="mt-3 text-lg font-bold">{detail.title}</h3>
        <p className="mt-1 text-sm text-gray-500">익명 출품</p>
        <p className="mt-2 whitespace-pre-wrap text-sm text-gray-700">
          {unescapeNewlines(detail.introduction)}
        </p>
        <div className="mt-4 flex justify-end gap-2">
          {voting && !detail.mine && (
            <button
              type="button"
              onClick={() => {
                toggle(detail.id);
                setDetail(null);
              }}
              className="rounded-lg bg-forest-600 px-4 py-2 text-sm text-white"
            >
              {selected.includes(detail.id) ? "선택 해제" : "투표 선택"}
            </button>
          )}
          <button
            type="button"
            onClick={() => setDetail(null)}
            className="rounded-lg border px-4 py-2 text-sm"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );

  const galleryCards = (works: PhotoExhibitionSubmission[]) => (
    <div className="grid gap-4 sm:grid-cols-2">
      {works.length === 0 && (
        <p className="col-span-full py-8 text-center text-sm text-gray-500">아직 출품작이 없습니다.</p>
      )}
      {works.map((work) => (
        <article key={work.id} className="overflow-hidden rounded-xl border bg-white">
          <button type="button" onClick={() => setDetail(work)} className="block w-full text-left">
            <div className="flex justify-center bg-gray-100">
              <img
                src={toMediaUrl(work.images[0]?.imageUrl, "md")}
                alt=""
                className="max-h-[28rem] w-auto max-w-full object-contain"
              />
            </div>
            <div className="p-3">
              <b className="text-gray-900">{work.title}</b>
              <p className="text-xs text-gray-500">익명 출품 · 사진 {work.images.length}장</p>
              {work.introduction && (
                <p className="mt-1 line-clamp-3 text-sm text-gray-700 whitespace-pre-wrap">
                  {unescapeNewlines(work.introduction)}
                </p>
              )}
            </div>
          </button>
          {voting && (
            <button
              type="button"
              disabled={work.mine}
              onClick={() => toggle(work.id)}
              className={`m-2 rounded-lg border px-3 py-1.5 text-sm ${
                work.mine
                  ? "cursor-not-allowed text-gray-400"
                  : selected.includes(work.id)
                    ? "border-forest-600 bg-forest-50 text-forest-700"
                    : ""
              }`}
            >
              {work.mine ? "내 작품" : selected.includes(work.id) ? "선택됨" : "선택"}
            </button>
          )}
        </article>
      ))}
    </div>
  );

  return (
    <section className="space-y-4">
      {error && <p className="text-sm text-forest-700">{error}</p>}

      {editable && (
        <>
          <div className="flex rounded-lg border bg-white p-1">
            {(
              [
                { key: "mine", label: "내 출품" },
                { key: "others", label: "다른 작품" },
              ] as const
            ).map((t) => (
              <button
                key={t.key}
                type="button"
                onClick={() => setTab(t.key)}
                className={`flex-1 rounded-md py-2 text-sm font-medium ${
                  tab === t.key ? "bg-forest-600 text-white" : "text-gray-600 hover:bg-gray-50"
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          {tab === "mine" && (
            <div className="rounded-xl border bg-white p-4">
              <h2 className="font-bold">내 출품작</h2>
              <input
                value={title}
                maxLength={150}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="작품 제목 (필수)"
                className="mt-3 w-full rounded border p-2"
              />
              <textarea
                value={introduction}
                maxLength={2000}
                onChange={(e) => setIntroduction(e.target.value)}
                placeholder="작품 소개 (필수)"
                className="mt-2 w-full rounded border p-2"
                rows={4}
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
                    >
                      <img
                        src={toMediaUrl(image.imageUrl, "sm")}
                        alt=""
                        className="aspect-square w-full object-cover"
                      />
                      {image.representative ? (
                        <span className="absolute left-1.5 top-1.5 inline-flex items-center gap-1 rounded-full bg-forest-600 px-2 py-0.5 text-[11px] font-semibold text-white shadow">
                          {starIcon} 대표
                        </span>
                      ) : (
                        <span className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-1 bg-black/55 py-1.5 text-[11px] font-medium text-white">
                          {starIcon} 대표로
                        </span>
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => remove(image.id)}
                      disabled={saving}
                      className="absolute right-1.5 top-1.5 rounded-full bg-white/95 px-1.5 py-0.5 text-[11px] font-medium text-red-600 shadow"
                    >
                      삭제
                    </button>
                  </div>
                ))}
                {(submission?.images.length || 0) < 4 && (
                  <label className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-gray-300 text-sm text-gray-500 hover:border-forest-400 hover:bg-forest-50">
                    <span className="text-lg">+</span>
                    사진 추가
                    <input type="file" accept="image/*" className="hidden" onChange={upload} />
                  </label>
                )}
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                <button
                  type="button"
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
                    className="rounded border border-red-300 px-3 py-2 text-sm text-red-600"
                  >
                    출품 삭제
                  </button>
                )}
              </div>
            </div>
          )}

          {tab === "others" && galleryCards(others)}
        </>
      )}

      {voting && (
        <>
          <div className="rounded-lg bg-forest-50 p-3 text-sm text-forest-800">
            마음에 드는 작품 최대 3개 선택 · 투표 보상 10/20/30💧 · 자기 작품 투표 불가
          </div>
          {galleryCards(gallery)}
          <div className="sticky bottom-2 flex items-center justify-between gap-2 rounded-xl bg-forest-700 p-3 text-white shadow-lg">
            <span>
              {selected.length}/3 · {selected.length * 10}💧
            </span>
            <div className="flex gap-2">
              <button type="button" onClick={cancelVotes} className="rounded-lg bg-white/20 px-3 py-1.5 text-sm">
                전체 취소
              </button>
              <button type="button" onClick={saveVotes} className="rounded-lg bg-white px-3 py-1.5 text-sm font-semibold text-forest-700">
                투표 저장
              </button>
            </div>
          </div>
        </>
      )}

      {detailModal}
    </section>
  );
}
