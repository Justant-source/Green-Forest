"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { login, findLoginIdsByName } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext";

export default function LoginPage() {
  const router = useRouter();
  const { refresh } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showPasswordHelp, setShowPasswordHelp] = useState(false);

  const [showFindId, setShowFindId] = useState(false);
  const [findName, setFindName] = useState("");
  const [findError, setFindError] = useState("");
  const [findLoading, setFindLoading] = useState(false);
  const [findResults, setFindResults] = useState<{ name: string; loginId: string }[] | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      await login(email, password);
      refresh();
      router.push("/");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const openFindId = () => {
    setShowFindId(true);
    setFindName("");
    setFindError("");
    setFindResults(null);
  };

  const handleFindId = async (e: React.FormEvent) => {
    e.preventDefault();
    setFindError("");
    setFindResults(null);
    if (!findName.trim()) {
      setFindError("이름을 입력해주세요.");
      return;
    }
    setFindLoading(true);
    try {
      const results = await findLoginIdsByName(findName);
      setFindResults(results);
    } catch (err: unknown) {
      setFindError(err instanceof Error ? err.message : "아이디 찾기에 실패했습니다.");
    } finally {
      setFindLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-10">
      <h1 className="text-2xl font-bold mb-6 text-center">로그인</h1>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-600 rounded-lg text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            아이디
          </label>
          <input
            type="text"
            value={email}
            onChange={(e) => setEmail(e.target.value.slice(0, 20))}
            placeholder="아이디를 입력하세요"
            autoComplete="username"
            maxLength={20}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
          <div className="mt-1.5 flex justify-end gap-3">
            <button
              type="button"
              onClick={openFindId}
              className="text-xs text-forest-500 hover:underline"
            >
              아이디 찾기
            </button>
            <button
              type="button"
              onClick={() => setShowPasswordHelp(true)}
              className="text-xs text-forest-500 hover:underline"
            >
              비밀번호 찾기
            </button>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            비밀번호
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full py-2 bg-forest-500 text-white rounded-lg font-medium hover:bg-forest-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-500">
        계정이 없으신가요?{" "}
        <Link href="/register" className="text-forest-500 hover:underline">
          회원가입
        </Link>
      </p>

      {showPasswordHelp && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
          onClick={() => setShowPasswordHelp(false)}
        >
          <div
            className="w-full max-w-sm bg-white rounded-xl shadow-lg p-5 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-base font-semibold text-gray-800">비밀번호 찾기</h2>
            <p className="text-sm text-gray-600">
              비밀번호 재설정은 관리자에게 문의해 주세요.
            </p>
            <button
              type="button"
              onClick={() => setShowPasswordHelp(false)}
              className="w-full py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600"
            >
              확인
            </button>
          </div>
        </div>
      )}

      {showFindId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
          onClick={() => setShowFindId(false)}
        >
          <div
            className="w-full max-w-sm bg-white rounded-xl shadow-lg p-5 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-base font-semibold text-gray-800">아이디 찾기</h2>
            <form onSubmit={handleFindId} className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
                <input
                  type="text"
                  value={findName}
                  onChange={(e) => setFindName(e.target.value)}
                  placeholder="가입 시 입력한 이름"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-500"
                  autoFocus
                />
              </div>
              {findError && (
                <p className="text-sm text-red-600">{findError}</p>
              )}
              {findResults !== null && (
                findResults.length === 0 ? (
                  <p className="text-sm text-gray-500">해당 이름으로 등록된 아이디가 없습니다.</p>
                ) : (
                  <ul className="space-y-2">
                    {findResults.map((r) => (
                      <li
                        key={r.loginId}
                        className="flex items-center justify-between gap-2 px-3 py-2 bg-gray-50 rounded-lg border border-gray-100"
                      >
                        <div className="min-w-0">
                          <div className="text-xs text-gray-400">{r.name}</div>
                          <div className="text-sm font-semibold text-gray-800 truncate">{r.loginId}</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setEmail(r.loginId.slice(0, 20));
                            setShowFindId(false);
                          }}
                          className="shrink-0 text-xs text-forest-500 hover:underline"
                        >
                          입력
                        </button>
                      </li>
                    ))}
                  </ul>
                )
              )}
              <div className="flex gap-2 pt-1">
                <button
                  type="submit"
                  disabled={findLoading}
                  className="flex-1 py-2 bg-forest-500 text-white rounded-lg text-sm font-medium hover:bg-forest-600 disabled:opacity-50"
                >
                  {findLoading ? "검색 중..." : "검색"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowFindId(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  닫기
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
