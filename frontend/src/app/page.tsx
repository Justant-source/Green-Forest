"use client";

import { Suspense } from "react";
import GridFeed from "@/components/GridFeed";
import GachaRecentWinsTicker from "@/components/GachaRecentWinsTicker";
import WeeklyReportBanner from "@/components/WeeklyReportBanner";
import EventModeBanner from "@/components/events/EventModeBanner";
import { useEventMode } from "@/context/EventModeContext";

function HomeContent() {
  const { mode } = useEventMode();
  const isEvent = mode === "ACTIVE";

  return (
    <div className="space-y-4">
      {isEvent && <EventModeBanner />}
      {!isEvent && <WeeklyReportBanner />}
      {!isEvent && <GachaRecentWinsTicker />}
      <GridFeed initialCategory={null} />
    </div>
  );
}

export default function HomePage() {
  return (
    <Suspense fallback={<div className="flex justify-center py-20"><div className="w-10 h-10 border-4 border-gray-300 border-t-forest-500 rounded-full animate-spin" /></div>}>
      <HomeContent />
    </Suspense>
  );
}
