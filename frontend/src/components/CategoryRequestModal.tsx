"use client";

import { useState } from "react";
import { requestCategory } from "@/lib/api";

interface CategoryRequestModalProps {
  onClose: () => void;
}

export default function CategoryRequestModal({ onClose }: CategoryRequestModalProps) {
  const [name, setName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setSubmitting(true);
    try {
      await requestCategory({ name: name.trim() });
      setSuccess(true);
    } catch (error) {
      console.error("Failed to request category:", error);
      alert("게시판 요청에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4 space-y-4" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold">게시판 요청</h2>

        {success ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              게시판 요청이 등록되었습니다. 관리자 승인 후 사용할 수 있습니다.
            </p>
            <button
              onClick={onClose}
              className="w-full py-2 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 transition-colors"
            >
              확인
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                게시판 이름
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="예: 여행, 맛집, 입양"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                required
              />
              <p className="text-xs text-gray-400 mt-1">관리자가 검토 후 게시판 설정을 완료합니다.</p>
            </div>
            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
              >
                취소
              </button>
              <button
                type="submit"
                disabled={submitting || !name.trim()}
                className="flex-1 py-2 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {submitting ? "요청 중..." : "요청하기"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
