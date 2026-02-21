"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { jua } from "@/lib/fonts";

export default function Header() {
  const { nickname, isLoggedIn, isAdmin, handleLogout } = useAuth();

  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-200/60">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 group">
          <svg
            className="w-7 h-7 text-orange-400 group-hover:text-orange-500 transition-colors"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <circle cx="6" cy="4.5" r="2.2" />
            <circle cx="12" cy="3" r="2.2" />
            <circle cx="18" cy="4.5" r="2.2" />
            <ellipse cx="12" cy="13" rx="5.5" ry="6" />
          </svg>
          <span className={`${jua.className} tracking-widest text-2xl text-gray-900 transition-colors`}>
            냐옹스
          </span>
        </Link>
        <div className="flex items-center gap-2">
          {isLoggedIn ? (
            <>
              <Link
                href="/profile"
                className="text-sm text-gray-500 hover:text-gray-900 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors"
              >
                {nickname}
              </Link>
              <Link
                href="/conversations"
                className="text-sm text-gray-500 hover:text-gray-900 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors"
              >
                대화목록
              </Link>
              {isAdmin && (
                <Link
                  href="/admin"
                  className="text-sm text-orange-500 hover:text-orange-600 px-3 py-1.5 rounded-full hover:bg-orange-50 transition-colors"
                >
                  관리자
                </Link>
              )}
              <Link
                href="/posts/new"
                className="bg-orange-400 hover:bg-orange-500 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors"
              >
                새 글 쓰기
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-400 hover:text-gray-600 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link
              href="/login"
              className="bg-orange-400 hover:bg-orange-500 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors"
            >
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
