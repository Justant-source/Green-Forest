"use client";

import { useRef } from "react";

interface Props {
  theme: string;
  open: boolean;
  onClose: () => void;
  onPick: (file: File) => void;
}

export default function CellUploadSheet({ theme, open, onClose, onPick }: Props) {
  const cameraRef = useRef<HTMLInputElement>(null);
  const galleryRef = useRef<HTMLInputElement>(null);

  if (!open) return null;

  const handle = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) onPick(f);
    e.target.value = "";
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50" onClick={onClose}>
      <div
        className="absolute bottom-0 inset-x-0 bg-white rounded-t-2xl p-4 space-y-2"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="text-center pb-2 mb-1 border-b border-gray-100">
          <div className="text-sm font-semibold text-gray-900">{theme}</div>
        </div>
        <button
          type="button"
          onClick={() => cameraRef.current?.click()}
          className="w-full py-3 text-base rounded-xl bg-forest-500 hover:bg-forest-600 text-white font-medium"
        >
          📷 카메라로 찍기
        </button>
        <button
          type="button"
          onClick={() => galleryRef.current?.click()}
          className="w-full py-3 text-base rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-800 font-medium"
        >
          🖼️ 앨범에서 선택
        </button>
        <button
          type="button"
          onClick={onClose}
          className="w-full py-3 text-base rounded-xl text-gray-500 hover:text-gray-700"
        >
          취소
        </button>
        <input
          ref={cameraRef}
          type="file"
          accept="image/*"
          capture="environment"
          hidden
          onChange={handle}
        />
        <input ref={galleryRef} type="file" accept="image/*" hidden onChange={handle} />
      </div>
    </div>
  );
}
