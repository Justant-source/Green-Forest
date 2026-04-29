"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import {
  getPost,
  getSurveyByPost,
  updateSurveyMeta,
  updateSurveyOption,
  deleteSurveyOption,
  addAdminSurveyOption,
} from "@/lib/api";
import { toMediaUrl } from "@/lib/api";
import { Survey, SurveyOption } from "@/types";

export default function SurveyEditPage() {
  const { id } = useParams<{ id: string }>();
  const postId = Number(id);
  const router = useRouter();
  const { isAdmin, authLoaded } = useAuth();

  const [survey, setSurvey] = useState<Survey | null>(null);
  const [title, setTitle] = useState("");
  const [closesAt, setClosesAt] = useState("");
  const [optionTexts, setOptionTexts] = useState<Record<number, string>>({});
  const [optionImages, setOptionImages] = useState<Record<number, File>>({});
  const [optionPreviews, setOptionPreviews] = useState<Record<number, string>>({});
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  // 새 옵션 추가 상태
  const [newText, setNewText] = useState("");
  const [newImage, setNewImage] = useState<File | null>(null);
  const [newPreview, setNewPreview] = useState<string | null>(null);
  const newFileRef = useRef<HTMLInputElement>(null);

  const loadData = async (preserveEdits = false) => {
    const [post, sv] = await Promise.all([getPost(postId), getSurveyByPost(postId)]);
    setTitle((prev) => (preserveEdits ? prev : post.title));
    const dt = new Date(sv.closesAt.endsWith("Z") ? sv.closesAt : sv.closesAt + "Z");
    const pad = (n: number) => String(n).padStart(2, "0");
    const newClosesAt = `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
    setClosesAt((prev) => (preserveEdits ? prev : newClosesAt));
    setSurvey(sv);

    if (preserveEdits) {
      // 기존 편집 내용 유지 + 새 옵션은 서버 값으로, 삭제된 옵션은 제거
      const validIds = new Set(sv.options.map((o) => o.id));
      setOptionTexts((prev) => {
        const merged: Record<number, string> = {};
        sv.options.forEach((o) => {
          merged[o.id] = o.id in prev ? prev[o.id] : (o.text ?? "");
        });
        return merged;
      });
      setOptionImages((prev) =>
        Object.fromEntries(Object.entries(prev).filter(([k]) => validIds.has(Number(k))))
      );
      setOptionPreviews((prev) =>
        Object.fromEntries(Object.entries(prev).filter(([k]) => validIds.has(Number(k))))
      );
    } else {
      const texts: Record<number, string> = {};
      sv.options.forEach((o) => { texts[o.id] = o.text ?? ""; });
      setOptionTexts(texts);
    }
  };

  useEffect(() => {
    if (!authLoaded) return;
    if (!isAdmin) { router.replace(`/posts/${postId}`); return; }
    loadData()
      .catch(() => router.replace(`/posts/${postId}`))
      .finally(() => setLoading(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoaded, isAdmin, postId]);

  const handleImageChange = (optionId: number, file: File) => {
    setOptionImages((prev) => ({ ...prev, [optionId]: file }));
    setOptionPreviews((prev) => ({ ...prev, [optionId]: URL.createObjectURL(file) }));
  };

  const handleDelete = async (optionId: number) => {
    if (!survey) return;
    if (!confirm("이 옵션을 삭제하시겠습니까?\n해당 옵션의 투표도 함께 삭제됩니다.")) return;
    try {
      await deleteSurveyOption(survey.id, optionId);
      await loadData(true);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "삭제 실패");
    }
  };

  const handleAddOption = async () => {
    if (!survey) return;
    if (!newText.trim() && !newImage) { alert("텍스트 또는 이미지를 입력해주세요."); return; }
    try {
      await addAdminSurveyOption(survey.id, newText.trim() || null, newImage);
      setNewText("");
      setNewImage(null);
      setNewPreview(null);
      await loadData(true);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "옵션 추가 실패");
    }
  };

  const handleSave = async () => {
    if (!survey) return;
    if (newText.trim() || newImage) {
      alert("추가 중인 옵션이 있습니다.\n먼저 \"+ 옵션 추가\" 버튼을 눌러 항목을 등록하거나 입력을 지워주세요.");
      return;
    }
    setSaving(true);
    try {
      const closesAtIso = new Date(closesAt).toISOString().slice(0, 19);
      await updateSurveyMeta(survey.id, title, closesAtIso);

      const adminOptions = survey.options.filter((o) => !o.addedByUser);
      await Promise.all(
        adminOptions.map((o) => {
          const newTextVal = o.type !== "IMAGE_ONLY" ? (optionTexts[o.id] ?? o.text ?? "") : null;
          const newImg = optionImages[o.id] ?? null;
          if (newTextVal === (o.text ?? "") && !newImg) return Promise.resolve();
          return updateSurveyOption(survey.id, o.id, newTextVal, newImg);
        })
      );

      router.push(`/posts/${postId}`);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "저장 실패");
      setSaving(false);
    }
  };

  if (!authLoaded || loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" />
      </div>
    );
  }

  if (!survey) return null;

  const adminOptions = survey.options.filter((o) => !o.addedByUser);
  const userOptions = survey.options.filter((o) => o.addedByUser);

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-12">
      <h1 className="text-xl font-bold">설문 수정</h1>

      {/* 제목 */}
      <div className="space-y-1">
        <label className="text-sm font-medium text-gray-700">제목</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
        />
      </div>

      {/* 종료일 */}
      <div className="space-y-1">
        <label className="text-sm font-medium text-gray-700">종료일</label>
        <input
          type="datetime-local"
          value={closesAt}
          onChange={(e) => setClosesAt(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
        />
        {survey.closed && (
          <p className="text-xs text-red-500">종료된 설문입니다. 종료일을 미래로 변경하면 재개됩니다.</p>
        )}
      </div>

      {/* 관리자 옵션 */}
      <div className="space-y-3">
        <p className="text-sm font-medium text-gray-700">옵션 ({adminOptions.length}개)</p>
        {adminOptions.map((o, i) => (
          <OptionEditor
            key={o.id}
            index={i}
            option={o}
            text={optionTexts[o.id] ?? o.text ?? ""}
            preview={optionPreviews[o.id]}
            onTextChange={(v) => setOptionTexts((prev) => ({ ...prev, [o.id]: v }))}
            onImageChange={(f) => handleImageChange(o.id, f)}
            onDelete={() => handleDelete(o.id)}
          />
        ))}
      </div>

      {/* 참여자 추가 옵션 */}
      {userOptions.length > 0 && (
        <div className="space-y-2">
          <p className="text-sm font-medium text-gray-500">참여자가 추가한 옵션</p>
          {userOptions.map((o) => (
            <div key={o.id} className="flex items-center justify-between px-3 py-2 bg-gray-50 rounded-lg border border-gray-200">
              <span className="text-sm text-gray-600">{o.text}</span>
              <button
                onClick={() => handleDelete(o.id)}
                className="ml-3 text-xs text-red-500 hover:text-red-700 font-medium shrink-0"
              >
                삭제
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 새 옵션 추가 */}
      <div className="border border-dashed border-gray-300 rounded-lg p-4 space-y-3">
        <p className="text-sm font-medium text-gray-700">새 옵션 추가</p>
        <input
          type="text"
          value={newText}
          maxLength={50}
          onChange={(e) => setNewText(e.target.value)}
          placeholder="텍스트 입력 (50자 이내, 선택)"
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
        />
        <div className="flex items-center gap-3">
          {newPreview && (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img src={newPreview} alt="" className="w-16 h-16 object-cover rounded-md border border-gray-200" />
          )}
          <input
            type="file"
            accept="image/*"
            className="hidden"
            ref={newFileRef}
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) { setNewImage(f); setNewPreview(URL.createObjectURL(f)); }
            }}
          />
          <button
            type="button"
            onClick={() => newFileRef.current?.click()}
            className="px-3 py-1.5 text-sm border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
          >
            {newPreview ? "이미지 변경" : "이미지 첨부"}
          </button>
          {newPreview && (
            <button
              type="button"
              onClick={() => { setNewImage(null); setNewPreview(null); }}
              className="text-xs text-red-400 hover:text-red-600"
            >
              제거
            </button>
          )}
        </div>
        <button
          onClick={handleAddOption}
          className="w-full px-4 py-2 bg-indigo-50 text-indigo-700 rounded-lg text-sm font-medium hover:bg-indigo-100 transition-colors"
        >
          + 옵션 추가
        </button>
      </div>

      <div className="flex gap-3 pt-2">
        <button
          onClick={() => router.push(`/posts/${postId}`)}
          className="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          취소
        </button>
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex-1 px-4 py-2.5 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 disabled:opacity-50"
        >
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}

function OptionEditor({
  index,
  option,
  text,
  preview,
  onTextChange,
  onImageChange,
  onDelete,
}: {
  index: number;
  option: SurveyOption;
  text: string;
  preview?: string;
  onTextChange: (v: string) => void;
  onImageChange: (f: File) => void;
  onDelete: () => void;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const hasText = option.type !== "IMAGE_ONLY";
  const hasImage = option.type !== "TEXT_ONLY";
  const currentImage = preview ?? (option.imageUrl ? toMediaUrl(option.imageUrl, "sm") : null);

  return (
    <div className="border border-gray-200 rounded-lg p-3 space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-xs text-gray-400 font-medium">옵션 {index + 1}</p>
        <button
          onClick={onDelete}
          className="text-xs text-red-400 hover:text-red-600 font-medium"
        >
          삭제
        </button>
      </div>
      {hasText && (
        <input
          type="text"
          value={text}
          maxLength={50}
          onChange={(e) => onTextChange(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
          placeholder="옵션 텍스트 (50자 이내)"
        />
      )}
      {hasImage && (
        <div className="flex items-center gap-3">
          {currentImage && (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img src={currentImage} alt="" className="w-16 h-16 object-cover rounded-md border border-gray-200" />
          )}
          <div>
            <input
              type="file"
              accept="image/*"
              className="hidden"
              ref={fileRef}
              onChange={(e) => { const f = e.target.files?.[0]; if (f) onImageChange(f); }}
            />
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              className="px-3 py-1.5 text-sm border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              {currentImage ? "이미지 교체" : "이미지 선택"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
