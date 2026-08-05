"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getMe } from "@/lib/api";
import { User } from "@/types";
import Link from "next/link";

export default function ProfilePage() {
  const { isLoggedIn, authLoaded } = useAuth();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    if (authLoaded && !isLoggedIn) {
      router.replace("/login");
      return;
    }
    if (isLoggedIn) {
      getMe().then(setUser).catch(() => null);
    }
  }, [authLoaded, isLoggedIn, router]);

  if (!authLoaded || !user) {
    return <div className="flex items-center justify-center min-h-screen text-gray-400 text-sm">불러오는 중...</div>;
  }

  const hasShipping = !!(user.addressMain && user.phone);

  return (
    <main className="max-w-lg mx-auto px-4 py-8 space-y-4">
      <h1 className="text-xl font-bold text-gray-800">마이페이지</h1>

      <div className="grid gap-3">
        {/* 정보 카드 */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-4 space-y-1">
          <div className="text-xs text-gray-400">닉네임</div>
          <div className="text-sm font-medium text-gray-800">{user.nickname}</div>
          <div className="text-xs text-gray-400 pt-1">이름</div>
          <div className="text-sm font-medium text-gray-800">{user.name}</div>
          <div className="text-xs text-gray-400 pt-1">아이디</div>
          <div className="text-sm font-medium text-gray-800">{user.email}</div>
        </div>

        {/* 배송지 카드 */}
        <Link
          href="/profile/shipping"
          className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors"
        >
          <div>
            <div className="text-sm font-semibold text-gray-800">배송지 관리</div>
            {hasShipping ? (
              <div className="text-xs text-gray-500 mt-0.5 truncate max-w-[240px]">
                {user.zipcode ? `(${user.zipcode}) ` : ""}{user.addressMain}
                {user.addressDetail ? ` ${user.addressDetail}` : ""}
              </div>
            ) : (
              <div className="text-xs text-amber-600 mt-0.5">미등록 — 배송 대상 설문 응답 전 등록 필요</div>
            )}
          </div>
          <span className="text-gray-400 text-lg">›</span>
        </Link>

        {/* 생일 설정 | 비밀번호 변경 */}
        <div className="grid grid-cols-2 gap-3">
          <Link
            href="/profile/birthday"
            className="bg-white rounded-2xl border border-gray-100 shadow-sm px-4 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors min-w-0"
          >
            <div className="min-w-0">
              <div className="text-sm font-semibold text-gray-800">생일 설정</div>
              <div className="text-xs text-gray-500 mt-0.5 truncate">
                {user.birthMonth && user.birthDay
                  ? `${String(user.birthMonth).padStart(2, "0")}/${String(user.birthDay).padStart(2, "0")}`
                  : "미등록"}
              </div>
            </div>
            <span className="text-gray-400 text-lg shrink-0">›</span>
          </Link>

          <Link
            href="/profile/password"
            className="bg-white rounded-2xl border border-gray-100 shadow-sm px-4 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors min-w-0"
          >
            <div className="text-sm font-semibold text-gray-800">비밀번호 변경</div>
            <span className="text-gray-400 text-lg shrink-0">›</span>
          </Link>
        </div>
      </div>
    </main>
  );
}
