"use client";

import { useEffect, useState } from "react";
import { getMyWeeklyReport } from "@/lib/api-weekly-report";
import { WeeklyReport } from "@/types/weekly-report";
import { isLoggedIn } from "@/lib/auth";

export default function WeeklyReportBanner() {
  const [report, setReport] = useState<WeeklyReport | null>(null);
  const [dismissed, setDismissed] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchReport = async () => {
      if (!isLoggedIn()) {
        setLoading(false);
        return;
      }

      const data = await getMyWeeklyReport();
      if (data) {
        const storedDismissedWeek = localStorage.getItem("dismissedWeeklyReport");
        if (storedDismissedWeek === data.weekStart) {
          setDismissed(true);
        } else {
          setReport(data);
        }
      }
      setLoading(false);
    };

    fetchReport();
  }, []);

  const handleDismiss = () => {
    if (report) {
      localStorage.setItem("dismissedWeeklyReport", report.weekStart);
      setDismissed(true);
    }
  };

  if (loading || dismissed || !report) {
    return null;
  }

  return (
    <div className="bg-gradient-to-r from-forest-100 to-forest-50 border-l-4 border-forest-500 rounded-lg p-4 mb-4 shadow-md">
      <div className="flex items-center justify-between gap-4">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-forest-700 mb-1">
            🌱 지난주 팀 기여
          </h3>
          <p className="text-forest-600">
            <span className="font-bold text-forest-700">{report.earnedAmount}</span>
            {" "}물방울을 모았습니다!
            {report.partyRank && (
              <span className="ml-3 text-forest-600">
                파티 내 <span className="font-bold text-forest-700">{report.partyRank}위</span>
              </span>
            )}
          </p>
        </div>
        <button
          onClick={handleDismiss}
          className="px-4 py-2 bg-forest-500 hover:bg-forest-600 text-white rounded-md font-medium transition-colors whitespace-nowrap"
        >
          확인
        </button>
      </div>
    </div>
  );
}
