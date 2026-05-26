"use client";

import { useEffect, useRef } from "react";

const SCRIPT_SRC =
  "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";

export type AddressResult = { zipcode: string; addressMain: string };

declare global {
  interface Window {
    daum?: { Postcode: new (opts: { oncomplete: (data: DaumPostcodeResult) => void }) => { open: () => void } };
  }
}

interface DaumPostcodeResult {
  zonecode: string;
  roadAddress: string;
  jibunAddress: string;
}

export default function AddressSearchButton({
  onSelect,
}: {
  onSelect: (r: AddressResult) => void;
}) {
  const loaded = useRef(false);

  useEffect(() => {
    if (window.daum?.Postcode) {
      loaded.current = true;
      return;
    }
    const s = document.createElement("script");
    s.src = SCRIPT_SRC;
    s.async = true;
    s.onload = () => {
      loaded.current = true;
    };
    document.body.appendChild(s);
  }, []);

  const open = () => {
    if (!window.daum?.Postcode) {
      alert("주소 검색 모듈 로드 중입니다. 잠시 후 다시 시도해 주세요.");
      return;
    }
    new window.daum.Postcode({
      oncomplete: (data) =>
        onSelect({
          zipcode: data.zonecode,
          addressMain: data.roadAddress || data.jibunAddress,
        }),
    }).open();
  };

  return (
    <button
      type="button"
      onClick={open}
      className="px-3 py-2 rounded bg-forest-500 text-white text-sm whitespace-nowrap hover:bg-forest-600 transition-colors"
    >
      주소 검색
    </button>
  );
}
