"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { compressImage } from "@/lib/imageCompression";
import { createSurvey } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { datetimeLocalToNaive } from "@/lib/datetime";

const MAX_OPTIONS = 15;
const MAX_DESCRIPTION = 300;

interface OptionDraft {
  text: string;
  description: string;
  imageFile: File | null;
  imagePreview: string | null;
}

const newOption = (): OptionDraft => ({
  text: "",
  description: "",
  imageFile: null,
  imagePreview: null,
});

export default function SurveyCreateForm({ onCancel }: { onCancel: () => void }) {
  const router = useRouter();
  const { isAdmin } = useAuth();
  const [title, setTitle] = useState("");
  const [closesAt, setClosesAt] = useState("");
  const [anonymous, setAnonymous] = useState(false);
  const [allowOptionAddByUser, setAllowOptionAddByUser] = useState(false);
  const [allowMultiSelect, setAllowMultiSelect] = useState(false);
  const [notice, setNotice] = useState(false);
  const [requiresShipping, setRequiresShipping] = useState(false);
  const [options, setOptions] = useState<OptionDraft[]>([newOption(), newOption()]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const updateOption = (i: number, patch: Partial<OptionDraft>) => {
    setOptions((prev) => prev.map((o, idx) => (idx === i ? { ...o, ...patch } : o)));
  };

  const addOption = () => {
    if (options.length >= MAX_OPTIONS) return;
    setOptions([...options, newOption()]);
  };

  const removeOption = (i: number) => {
    if (options.length <= 2) return;
    setOptions(options.filter((_, idx) => idx !== i));
  };

  const handleImageChange = async (i: number, file: File | null) => {
    if (!file) {
      updateOption(i, { imageFile: null, imagePreview: null });
      return;
    }
    const compressed = await compressImage(file);
    const preview = URL.createObjectURL(compressed);
    updateOption(i, { imageFile: compressed, imagePreview: preview });
  };

  const validate = (): string | null => {
    if (!title.trim()) return "제목을 입력하세요.";
    if (!closesAt) return "종료일을 입력하세요.";
    if (new Date(closesAt) <= new Date()) return "종료일은 미래여야 합니다.";
    if (options.length < 2) return "항목은 최소 2개 필요합니다.";
    for (let i = 0; i < options.length; i++) {
      const o = options[i];
      if (!o.text.trim()) return `${i + 1}번 항목의 제목을 입력하세요.`;
      if (o.description.length > MAX_DESCRIPTION)
        return `${i + 1}번 항목의 설명은 ${MAX_DESCRIPTION}자 이하여야 합니다.`;
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    setError("");
    setSubmitting(true);

    try {
      const fd = new FormData();
      fd.append("title", title.trim());
      fd.append("closesAt", datetimeLocalToNaive(closesAt));
      fd.append("anonymous", String(anonymous));
      fd.append("allowOptionAddByUser", String(isAdmin && allowOptionAddByUser));
      fd.append("allowMultiSelect", isAdmin && requiresShipping ? "false" : String(allowMultiSelect));
      fd.append("notice", String(isAdmin && notice));
      fd.append("requiresShipping", String(isAdmin && requiresShipping));
      const optionsJson = options.map((o) => ({
        type: o.imageFile ? "TEXT_AND_IMAGE" : "TEXT_ONLY",
        text: o.text.trim(),
        description: o.description.trim() || null,
      }));
      fd.append("options", JSON.stringify(optionsJson));
      options.forEach((o, i) => {
        if (o.imageFile) fd.append(`optionImage_${i}`, o.imageFile);
      });

      const { postId } = await createSurvey(fd);
      sessionStorage.removeItem("gridFeedCache");
      router.push(`/posts/${postId}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "투표 생성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">투표 만들기</h2>
        <button type="button" onClick={onCancel} className="text-xs text-forest-600 underline">
          카테고리 변경
        </button>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">제목</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="투표 제목을 입력하세요"
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">종료일 및 시간</label>
        <input
          type="datetime-local"
          value={closesAt}
          onChange={(e) => setClosesAt(e.target.value)}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
          required
        />
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium text-gray-700">
            항목 ({options.length}/{MAX_OPTIONS})
          </label>
          <button
            type="button"
            onClick={addOption}
            disabled={options.length >= MAX_OPTIONS}
            className="text-xs px-3 py-1 bg-forest-50 text-forest-600 rounded-lg disabled:opacity-50"
          >
            + 추가
          </button>
        </div>

        <div className="space-y-3">
          {options.map((o, i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500">항목 {i + 1}</span>
                <button
                  type="button"
                  onClick={() => removeOption(i)}
                  disabled={options.length <= 2}
                  className="text-xs text-red-500 disabled:opacity-30"
                >
                  삭제
                </button>
              </div>

              <input
                type="text"
                value={o.text}
                onChange={(e) => updateOption(i, { text: e.target.value })}
                placeholder="항목 제목 (50자 이내)"
                maxLength={50}
                className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm"
              />

              <textarea
                value={o.description}
                onChange={(e) => updateOption(i, { description: e.target.value })}
                placeholder="설명 (선택, 300자 이내)"
                maxLength={MAX_DESCRIPTION}
                rows={2}
                className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm resize-none"
              />

              <div>
                <input
                  type="file"
                  accept=".png,.jpg,.jpeg"
                  onChange={(e) => handleImageChange(i, e.target.files?.[0] || null)}
                  className="text-xs"
                />
                {o.imagePreview && (
                  <div className="mt-2 flex items-start gap-2">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={o.imagePreview}
                      alt=""
                      className="w-24 h-24 object-cover rounded-md"
                    />
                    <button
                      type="button"
                      onClick={() => handleImageChange(i, null)}
                      className="text-xs text-gray-500 underline"
                    >
                      사진 제거
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-2 bg-gray-50 rounded-lg p-3">
        <Toggle label="익명 투표" checked={anonymous} onChange={setAnonymous} />
        <Toggle label="복수 항목 선택 허용" checked={allowMultiSelect} onChange={setAllowMultiSelect} disabled={isAdmin && requiresShipping} />
        {isAdmin && (
          <>
            <Toggle label="참여자 항목 추가 허용" checked={allowOptionAddByUser} onChange={setAllowOptionAddByUser} />
            <Toggle label="공지로 등록 (상단 배너 노출)" checked={notice} onChange={setNotice} />
            <Toggle
              label="상품 배송 대상 설문"
              checked={requiresShipping}
              onChange={(v) => { setRequiresShipping(v); if (v) setAllowMultiSelect(false); }}
            />
          </>
        )}
      </div>

      {error && <p className="text-sm text-red-500">{error}</p>}

      <div className="flex justify-end gap-3 pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-6 py-2 border border-gray-300 rounded-lg text-sm"
        >
          취소
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="px-6 py-2 bg-forest-500 text-white rounded-lg text-sm disabled:opacity-50"
        >
          {submitting ? "등록 중..." : "등록하기"}
        </button>
      </div>
    </form>
  );
}

function Toggle({
  label,
  checked,
  onChange,
  disabled,
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <label className={`flex items-center gap-2 text-sm text-gray-700 ${disabled ? "opacity-40 cursor-not-allowed" : "cursor-pointer"}`}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        disabled={disabled}
        className="w-4 h-4 accent-forest-500"
      />
      {label}
    </label>
  );
}
