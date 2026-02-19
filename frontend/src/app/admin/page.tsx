"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { CategoryInfo, CategoryRequestInfo } from "@/types";
import {
  getAdminCategories,
  createAdminCategory,
  deleteAdminCategory,
  getPendingCategoryRequests,
  approveCategoryRequest,
  rejectCategoryRequest,
} from "@/lib/api";

const COLOR_OPTIONS = [
  { value: "yellow", label: "노랑", cls: "bg-yellow-400" },
  { value: "blue", label: "파랑", cls: "bg-blue-400" },
  { value: "orange", label: "주황", cls: "bg-orange-400" },
  { value: "purple", label: "보라", cls: "bg-purple-400" },
  { value: "green", label: "초록", cls: "bg-green-400" },
  { value: "red", label: "빨강", cls: "bg-red-400" },
  { value: "pink", label: "분홍", cls: "bg-pink-400" },
  { value: "indigo", label: "남색", cls: "bg-indigo-400" },
  { value: "orange", label: "청록", cls: "bg-orange-400" },
  { value: "gray", label: "회색", cls: "bg-gray-400" },
];

const colorClsMap: Record<string, string> = Object.fromEntries(
  COLOR_OPTIONS.map((c) => [c.value, c.cls])
);

export default function AdminPage() {
  const router = useRouter();
  const { isLoggedIn, isAdmin } = useAuth();

  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [requests, setRequests] = useState<CategoryRequestInfo[]>([]);
  const [loading, setLoading] = useState(true);

  // 게시판 생성 폼
  const [newName, setNewName] = useState("");
  const [newLabel, setNewLabel] = useState("");
  const [newColor, setNewColor] = useState("blue");
  const [newHasStatus, setNewHasStatus] = useState(false);
  const [creating, setCreating] = useState(false);

  // 승인 폼
  const [approvingId, setApprovingId] = useState<number | null>(null);
  const [approveLabel, setApproveLabel] = useState("");
  const [approveColor, setApproveColor] = useState("blue");
  const [approveHasStatus, setApproveHasStatus] = useState(false);

  // 거절 사유
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  useEffect(() => {
    if (!isLoggedIn || !isAdmin) {
      router.replace("/");
      return;
    }
    loadData();
  }, [isLoggedIn, isAdmin, router]);

  const loadData = async () => {
    try {
      const [cats, reqs] = await Promise.all([
        getAdminCategories(),
        getPendingCategoryRequests(),
      ]);
      setCategories(cats);
      setRequests(reqs);
    } catch (error) {
      console.error("Failed to load admin data:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newLabel.trim()) return;

    setCreating(true);
    try {
      const created = await createAdminCategory({
        name: newName.trim().toUpperCase(),
        label: newLabel.trim(),
        color: newColor,
        hasStatus: newHasStatus,
      });
      setCategories((prev) => [...prev, created]);
      setNewName("");
      setNewLabel("");
      setNewColor("blue");
      setNewHasStatus(false);
    } catch (error) {
      console.error("Failed to create category:", error);
      alert("게시판 생성에 실패했습니다. 이미 존재하는 이름일 수 있습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number, label: string) => {
    if (!confirm(`"${label}" 게시판를 삭제하시겠습니까?`)) return;
    try {
      await deleteAdminCategory(id);
      setCategories((prev) => prev.filter((c) => c.id !== id));
    } catch (error) {
      console.error("Failed to delete category:", error);
      alert("게시판 삭제에 실패했습니다.");
    }
  };

  const handleApprove = async (id: number) => {
    if (!approveLabel.trim()) {
      alert("표시 이름을 입력하세요.");
      return;
    }
    try {
      const created = await approveCategoryRequest(id, {
        label: approveLabel.trim(),
        color: approveColor,
        hasStatus: approveHasStatus,
      });
      setRequests((prev) => prev.filter((r) => r.id !== id));
      setCategories((prev) => [...prev, created]);
      setApprovingId(null);
      setApproveLabel("");
      setApproveColor("blue");
      setApproveHasStatus(false);
    } catch (error) {
      console.error("Failed to approve:", error);
      alert("승인에 실패했습니다.");
    }
  };

  const handleReject = async (id: number) => {
    try {
      await rejectCategoryRequest(id, rejectReason);
      setRequests((prev) => prev.filter((r) => r.id !== id));
      setRejectingId(null);
      setRejectReason("");
    } catch (error) {
      console.error("Failed to reject:", error);
      alert("거절에 실패했습니다.");
    }
  };

  if (!isLoggedIn || !isAdmin) return null;

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-10 h-10 border-4 border-gray-300 border-t-orange-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-10">
      {/* 게시판 관리 */}
      <section>
        <h1 className="text-2xl font-bold mb-6">게시판 관리</h1>

        {/* 생성 폼 */}
        <form onSubmit={handleCreate} className="flex flex-wrap gap-2 mb-6">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="코드 (영문)"
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-28 focus:outline-none focus:ring-2 focus:ring-orange-500"
            required
          />
          <input
            type="text"
            value={newLabel}
            onChange={(e) => setNewLabel(e.target.value)}
            placeholder="표시 이름"
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-28 focus:outline-none focus:ring-2 focus:ring-orange-500"
            required
          />
          <select
            value={newColor}
            onChange={(e) => setNewColor(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg bg-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
          >
            {COLOR_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <label className="flex items-center gap-1.5 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={newHasStatus}
              onChange={(e) => setNewHasStatus(e.target.checked)}
              className="w-4 h-4 accent-orange-500"
            />
            상태관리
          </label>
          <button
            type="submit"
            disabled={creating || !newName.trim() || !newLabel.trim()}
            className="px-4 py-2 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {creating ? "생성 중..." : "추가"}
          </button>
        </form>

        {/* 게시판 목록 */}
        {categories.length === 0 ? (
          <p className="text-center text-gray-400 py-8">등록된 게시판가 없습니다.</p>
        ) : (
          <div className="space-y-2">
            {categories.map((cat) => (
              <div
                key={cat.id}
                className="flex items-center justify-between px-4 py-3 border border-gray-200 rounded-lg bg-white"
              >
                <div className="flex items-center gap-3">
                  <span className={`w-3 h-3 rounded-full ${colorClsMap[cat.color] || "bg-gray-400"}`} />
                  <span className="font-medium text-sm">{cat.label}</span>
                  <span className="text-xs text-gray-400">{cat.name}</span>
                  {cat.hasStatus && (
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-orange-100 text-orange-600 font-medium">상태</span>
                  )}
                </div>
                <button
                  onClick={() => handleDelete(cat.id, cat.label)}
                  className="text-sm text-red-500 hover:text-red-700 font-medium transition-colors"
                >
                  삭제
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 게시판 요청 관리 */}
      <section>
        <h2 className="text-2xl font-bold mb-6">게시판 요청 관리</h2>

        {requests.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            대기 중인 요청이 없습니다.
          </div>
        ) : (
          <div className="space-y-4">
            {requests.map((req) => (
              <div
                key={req.id}
                className="border border-gray-200 rounded-lg p-5 bg-white"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-semibold text-lg">{req.name}</span>
                    </div>
                    <div className="text-sm text-gray-500">
                      요청자: {req.requesterNickname} | {new Date(req.createdAt).toLocaleDateString("ko-KR")}
                    </div>
                  </div>
                  <span className="px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-800">
                    대기중
                  </span>
                </div>

                {rejectingId === req.id ? (
                  <div className="mt-4 space-y-2">
                    <textarea
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      placeholder="거절 사유를 입력하세요"
                      rows={2}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleReject(req.id)}
                        className="px-4 py-1.5 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors"
                      >
                        거절 확인
                      </button>
                      <button
                        onClick={() => {
                          setRejectingId(null);
                          setRejectReason("");
                        }}
                        className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        취소
                      </button>
                    </div>
                  </div>
                ) : approvingId === req.id ? (
                  <div className="mt-4 space-y-3">
                    <div className="flex flex-wrap gap-2">
                      <input
                        type="text"
                        value={approveLabel}
                        onChange={(e) => setApproveLabel(e.target.value)}
                        placeholder="표시 이름 (한글)"
                        className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-32 focus:outline-none focus:ring-2 focus:ring-orange-500"
                      />
                      <select
                        value={approveColor}
                        onChange={(e) => setApproveColor(e.target.value)}
                        className="px-3 py-2 border border-gray-300 rounded-lg bg-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                      >
                        {COLOR_OPTIONS.map((opt) => (
                          <option key={opt.value} value={opt.value}>
                            {opt.label}
                          </option>
                        ))}
                      </select>
                      <label className="flex items-center gap-1.5 text-sm text-gray-700 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={approveHasStatus}
                          onChange={(e) => setApproveHasStatus(e.target.checked)}
                          className="w-4 h-4 accent-orange-500"
                        />
                        상태관리
                      </label>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleApprove(req.id)}
                        className="px-4 py-1.5 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 transition-colors"
                      >
                        승인 확인
                      </button>
                      <button
                        onClick={() => {
                          setApprovingId(null);
                          setApproveLabel("");
                          setApproveColor("blue");
                          setApproveHasStatus(false);
                        }}
                        className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex gap-2 mt-4">
                    <button
                      onClick={() => setApprovingId(req.id)}
                      className="px-4 py-1.5 bg-orange-600 text-white rounded-lg text-sm font-medium hover:bg-orange-700 transition-colors"
                    >
                      승인
                    </button>
                    <button
                      onClick={() => setRejectingId(req.id)}
                      className="px-4 py-1.5 border border-red-300 text-red-600 rounded-lg text-sm font-medium hover:bg-red-50 transition-colors"
                    >
                      거절
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
