"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { jua } from "@/lib/fonts";

export default function Header() {
  const { nickname, isLoggedIn, isAdmin, authLoaded } = useAuth();

  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-200/60">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 group">
          <svg
            className="w-7 h-7 group-hover:scale-110 transition-transform"
            viewBox="0 0 24 24"
          >
            <rect x="10.5" y="17" width="3" height="5" rx="1" fill="#8B6914" />
            <ellipse cx="12" cy="13" rx="7" ry="5.5" fill="#2D8A4E" />
            <ellipse cx="12" cy="8" rx="5" ry="4.5" fill="#3DA35D" />
            <ellipse cx="12" cy="4.5" rx="3" ry="3" fill="#5BB37F" />
            <circle cx="9" cy="11" r="0.7" fill="#F5F9F0" opacity="0.5" />
            <circle cx="14" cy="8" r="0.5" fill="#F5F9F0" opacity="0.4" />
          </svg>
          <span className={`${jua.className} tracking-widest text-2xl text-gray-900 transition-colors`}>
            그린메이커
          </span>
        </Link>
        <div className="flex items-center gap-2">
          {!authLoaded ? (
            <div className="w-16 h-8" />
          ) : isLoggedIn ? (
            <>
              {isAdmin && (
                <Link
                  href="/admin"
                  className="text-sm text-forest-500 hover:text-forest-600 px-3 py-1.5 rounded-full hover:bg-forest-50 transition-colors"
                >
                  관리자
                </Link>
              )}
              <Link
                href="/posts/new"
                className="bg-forest-500 hover:bg-forest-600 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors"
              >
                새 글
              </Link>
              <Link
                href="/conversations"
                className="text-sm text-gray-500 hover:text-gray-900 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors hidden md:inline-flex"
              >
                대화
              </Link>
              <Link
                href="/garden"
                className="text-sm text-gray-500 hover:text-gray-900 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors hidden md:inline-flex"
              >
                {nickname}
              </Link>
            </>
          ) : (
            <Link
              href="/login"
              className="bg-forest-500 hover:bg-forest-600 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors"
            >
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
