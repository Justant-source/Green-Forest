"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getSurveyNotices } from "@/lib/api";
import { SurveyNotice } from "@/types";

const HIDDEN_KEY = "hiddenSurveyNotices_v1";
const HIDE_DURATION_MS = 24 * 60 * 60 * 1000;

export default function SurveyNoticeBanner() {
  const [notices, setNotices] = useState<SurveyNotice[]>([]);

  useEffect(() => {
    let mounted = true;
    getSurveyNotices()
      .then((list) => {
        if (!mounted) return;
        const now = Date.now();
        const hidden: Record<string, string> = JSON.parse(
          localStorage.getItem(HIDDEN_KEY) || "{}"
        );
        for (const k of Object.keys(hidden)) {
          if (now - new Date(hidden[k]).getTime() > HIDE_DURATION_MS) delete hidden[k];
        }
        localStorage.setItem(HIDDEN_KEY, JSON.stringify(hidden));
        setNotices(list.filter((n) => !hidden[String(n.surveyId)]));
      })
      .catch(() => {});
    return () => {
      mounted = false;
    };
  }, []);

  const dismiss = (surveyId: number) => {
    const hidden: Record<string, string> = JSON.parse(
      localStorage.getItem(HIDDEN_KEY) || "{}"
    );
    hidden[String(surveyId)] = new Date().toISOString();
    localStorage.setItem(HIDDEN_KEY, JSON.stringify(hidden));
    setNotices((prev) => prev.filter((n) => n.surveyId !== surveyId));
  };

  if (notices.length === 0) return null;

  return (
    <div className="space-y-1 mb-3">
      {notices.map((n) => (
        <div
          key={n.surveyId}
          className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 flex items-center gap-2"
        >
          <span className="text-amber-700 text-xs font-bold shrink-0">📢 투표공지</span>
          <span className="text-sm text-gray-800 truncate flex-1">"{n.title}"</span>
          <Link
            href={`/posts/${n.postId}`}
            className="text-xs text-amber-700 underline shrink-0"
          >
            바로가기
          </Link>
          <button
            onClick={() => dismiss(n.surveyId)}
            className="text-amber-400 hover:text-amber-700 text-xs ml-1 shrink-0"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}
