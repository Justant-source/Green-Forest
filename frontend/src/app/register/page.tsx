"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { register, getRegistrationOpen } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext";
import BirthMonthDaySelect from "@/components/BirthMonthDaySelect";
import { isValidBirthMonthDay } from "@/lib/birthMonthDay";

export default function RegisterPage() {
  const router = useRouter();
  const { refresh } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [birthMonth, setBirthMonth] = useState<number | "">("");
  const [birthDay, setBirthDay] = useState<number | "">("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [registrationOpen, setRegistrationOpen] = useState<boolean | null>(null);

  useEffect(() => {
    getRegistrationOpen()
      .then(setRegistrationOpen)
      .catch(() => setRegistrationOpen(true));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!email.trim()) {
      setError("아이디를 입력해주세요.");
      return;
    }
    if (email.trim().length > 20) {
      setError("아이디는 20자 이하여야 합니다.");
      return;
    }

    if (!isValidBirthMonthDay(birthMonth, birthDay)) {
      setError("생일을 선택해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      await register(email, password, nickname, name, birthMonth as number, birthDay as number);
      refresh();
      router.push("/");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "회원가입에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (registrationOpen === null) {
    return (
      <div className="max-w-md mx-auto mt-10 text-center text-sm text-gray-400">
        불러오는 중...
      </div>
    );
  }

  if (!registrationOpen) {
    return (
      <div className="max-w-md mx-auto mt-10">
        <h1 className="text-2xl font-bold mb-6 text-center">회원가입</h1>
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-800 text-center space-y-2">
          <p>현재 신규 가입을 받지 않습니다.</p>
          <p>관리자에게 문의해 주세요.</p>
        </div>
        <p className="mt-4 text-center text-sm text-gray-500">
          <Link href="/login" className="text-forest-500 hover:underline">
            로그인으로 돌아가기
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto mt-10">
      <h1 className="text-2xl font-bold mb-6 text-center">회원가입</h1>

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
            placeholder="아이디를 입력하세요 (최대 20자)"
            autoComplete="username"
            maxLength={20}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
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

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            이름
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="이름을 입력하세요"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            닉네임
          </label>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="닉네임을 입력하세요"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-forest-500"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            생일
          </label>
          <BirthMonthDaySelect
            month={birthMonth}
            day={birthDay}
            onChange={({ month, day }) => {
              setBirthMonth(month);
              setBirthDay(day);
            }}
            required
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full py-2 bg-forest-500 text-white rounded-lg font-medium hover:bg-forest-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? "가입 중..." : "회원가입"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-500">
        이미 계정이 있으신가요?{" "}
        <Link href="/login" className="text-forest-500 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}
