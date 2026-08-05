"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { getMe, updateMyProfile } from "@/lib/api";
import BirthMonthDaySelect from "@/components/BirthMonthDaySelect";
import { formatBirthMonthDay, isValidBirthMonthDay } from "@/lib/birthMonthDay";

export default function ProfileBirthdayPage() {
  const { isLoggedIn, authLoaded } = useAuth();
  const router = useRouter();
  const [month, setMonth] = useState<number | "">("");
  const [day, setDay] = useState<number | "">("");
  const [currentLabel, setCurrentLabel] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (authLoaded && !isLoggedIn) {
      router.replace("/login");
      return;
    }
    if (!isLoggedIn) return;
    getMe()
      .then((user) => {
        setMonth(user.birthMonth ?? "");
        setDay(user.birthDay ?? "");
        setCurrentLabel(formatBirthMonthDay(user.birthMonth, user.birthDay));
      })
      .catch(() => setError("생일 정보를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [authLoaded, isLoggedIn, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    if (!isValidBirthMonthDay(month, day)) {
      setError("생일을 선택해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      const user = await updateMyProfile({
        birthMonth: month as number,
        birthDay: day as number,
      });
      setCurrentLabel(formatBirthMonthDay(user.birthMonth, user.birthDay));
      setSuccess("생일이 저장되었습니다.");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!authLoaded || loading) {
    return <div className="flex items-center justify-center min-h-screen text-gray-400 text-sm">불러오는 중...</div>;
  }

  return (
    <main className="max-w-lg mx-auto px-4 py-8 space-y-4">
      <div className="flex items-center gap-2">
        <Link href="/profile" className="text-sm text-gray-400 hover:text-gray-600">‹ 마이페이지</Link>
      </div>
      <h1 className="text-xl font-bold text-gray-800">생일 설정</h1>

      {currentLabel && (
        <p className="text-sm text-gray-500">현재 생일: {currentLabel}</p>
      )}

      {error && (
        <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm">{error}</div>
      )}
      {success && (
        <div className="p-3 bg-green-50 text-green-700 rounded-lg text-sm">{success}</div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-4 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">생일 (월/일)</label>
          <BirthMonthDaySelect
            month={month}
            day={day}
            onChange={({ month: m, day: d }) => {
              setMonth(m);
              setDay(d);
            }}
            required
            idPrefix="profile-birth"
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full py-2 bg-forest-500 text-white rounded-lg font-medium hover:bg-forest-600 disabled:opacity-50"
        >
          {submitting ? "저장 중..." : "저장"}
        </button>
      </form>
    </main>
  );
}
