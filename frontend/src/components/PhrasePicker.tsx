"use client";
import React, { useState } from "react";
import { AttendancePhrase } from "@/types";

interface Props {
  phrases: AttendancePhrase[];
  onSelect: (value: string, phraseId?: number) => void;
}

export default function PhrasePicker({ phrases, onSelect }: Props) {
  const [customMode, setCustomMode] = useState(false);
  const [custom, setCustom] = useState("");
  const [selected, setSelected] = useState<number | null>(null);

  const handlePhraseClick = (p: AttendancePhrase) => {
    setSelected(p.id);
    setCustomMode(false);
    onSelect(p.phrase, p.id);
  };

  const handleCustomChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCustom(e.target.value);
    setSelected(null);
    onSelect(e.target.value, undefined);
  };

  return (
    <div className="space-y-3">
      <div className="flex gap-2 flex-wrap">
        {phrases.map((p) => (
          <button
            key={p.id}
            onClick={() => handlePhraseClick(p)}
            className={`px-3 py-1.5 rounded-full text-sm border transition-colors
              ${
                selected === p.id
                  ? "bg-green-500 text-white border-green-500"
                  : "bg-white text-gray-600 border-gray-200 hover:border-green-300"
              }`}
          >
            {p.phrase}
          </button>
        ))}
        <button
          onClick={() => {
            setCustomMode(!customMode);
            setSelected(null);
          }}
          className={`px-3 py-1.5 rounded-full text-sm border transition-colors
            ${
              customMode
                ? "bg-blue-500 text-white border-blue-500"
                : "bg-white text-gray-600 border-gray-200 hover:border-blue-300"
            }`}
        >
          직접 입력
        </button>
      </div>

      {customMode && (
        <input
          type="text"
          value={custom}
          onChange={handleCustomChange}
          placeholder="오늘의 한마디를 입력하세요 (최대 50자)"
          maxLength={50}
          className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-green-400"
        />
      )}
    </div>
  );
}
