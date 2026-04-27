"use client";

import { useState } from "react";
import { adminCreateEvent } from "@/lib/events/api";
import type { CreateEventRequest } from "@/lib/events/types";

const DEFAULT_THEMES = [
  "🌸 벚나무/봄꽃 1컷",
  "🐿️ 살아있는 동물 1컷",
  "🪑 가장 편한 벤치",
  "🏛️ 윤봉길 동상과 경례 샷",
  "🌿 특이한 모양의 잎",
  "👥 다른 파티원 2명과 셀카",
  "☁️ 나무 사이로 본 하늘",
  "🦶 초록 잔디 밟은 발 인증",
  "🎨 '초록'이 아닌 색의 자연물",
];

/** "2026-04-22T15:00" (브라우저 local datetime-input) → "2026-04-22T15:00:00" (서버 LocalDateTime) */
function toServerTime(v: string): string {
  if (!v) return v;
  return v.length === 16 ? v + ":00" : v;
}

interface Props {
  onCreated: () => void;
  onCancel: () => void;
}

export default function EventCreateForm({ onCreated, onCancel }: Props) {
  const [title, setTitle] = useState("매헌 포토 빙고");
  const [description, setDescription] = useState("매헌시민의숲에서 9가지 테마 사진을 찍어 빙고를 완성하세요!");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [themes, setThemes] = useState<string[]>(DEFAULT_THEMES);
  const [line3, setLine3] = useState(50);
  const [line5, setLine5] = useState(80);
  const [blackout, setBlackout] = useState(120);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setTheme = (i: number, v: string) => {
    setThemes((prev) => prev.map((t, idx) => (idx === i ? v : t)));
  };

  const submit = async () => {
    setError(null);
    if (!title.trim()) return setError("제목을 입력하세요.");
    if (!startAt || !endAt) return setError("시작/종료 시각을 입력하세요.");
    if (themes.some((t) => !t.trim())) return setError("테마 9개를 모두 입력하세요.");

    const payload: CreateEventRequest = {
      type: "PHOTO_BINGO",
      title: title.trim(),
      description: description.trim() || undefined,
      startAt: toServerTime(startAt),
      endAt: toServerTime(endAt),
      config: {
        themes: themes.map((t) => t.trim()),
        rewards: { line3, line5, blackout },
      },
    };

    setSaving(true);
    try {
      await adminCreateEvent(payload);
      onCreated();
    } catch (e: any) {
      setError(e?.message ?? "생성 실패");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">새 이벤트 생성</h3>
        <button onClick={onCancel} className="text-sm text-gray-500 hover:text-gray-700">
          ✕ 닫기
        </button>
      </div>
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-2">
          {error}
        </div>
      )}
      <div className="grid gap-3">
        <div>
          <label className="text-xs text-gray-600">제목</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="text-xs text-gray-600">설명</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="text-xs text-gray-600">시작 시각 (KST)</label>
            <input
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">종료 시각 (KST)</label>
            <input
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
            />
          </div>
        </div>

        <div>
          <div className="text-xs text-gray-600 mb-1">테마 9개</div>
          <div className="grid grid-cols-3 gap-2">
            {themes.map((t, i) => (
              <input
                key={i}
                value={t}
                onChange={(e) => setTheme(i, e.target.value)}
                className="border border-gray-200 rounded-lg px-2 py-1.5 text-xs"
                placeholder={`셀 ${i + 1}`}
              />
            ))}
          </div>
        </div>

        <div>
          <div className="text-xs text-gray-600 mb-1">보상 (💧 물방울)</div>
          <div className="grid grid-cols-3 gap-2">
            <label className="text-xs">
              3줄
              <input
                type="number"
                value={line3}
                onChange={(e) => setLine3(Number(e.target.value))}
                className="w-full border border-gray-200 rounded-lg px-2 py-1.5 text-sm"
              />
            </label>
            <label className="text-xs">
              5줄
              <input
                type="number"
                value={line5}
                onChange={(e) => setLine5(Number(e.target.value))}
                className="w-full border border-gray-200 rounded-lg px-2 py-1.5 text-sm"
              />
            </label>
            <label className="text-xs">
              블랙아웃
              <input
                type="number"
                value={blackout}
                onChange={(e) => setBlackout(Number(e.target.value))}
                className="w-full border border-gray-200 rounded-lg px-2 py-1.5 text-sm"
              />
            </label>
          </div>
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-2">
        <button
          onClick={onCancel}
          className="px-4 py-2 rounded-full bg-gray-100 hover:bg-gray-200 text-sm"
        >
          취소
        </button>
        <button
          onClick={submit}
          disabled={saving}
          className="px-4 py-2 rounded-full bg-forest-500 hover:bg-forest-600 text-white text-sm font-medium disabled:opacity-60"
        >
          {saving ? "생성 중..." : "생성"}
        </button>
      </div>
    </div>
  );
}
